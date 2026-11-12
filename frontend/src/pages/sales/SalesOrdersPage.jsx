import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, IconButton, Tooltip, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import CancelIcon from '@mui/icons-material/Cancel';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { salesOrderApi } from '../../api/salesOrderApi';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import useServerTable from '../../hooks/useServerTable';
import { useCustomers, useMaterials } from '../../hooks/useLookups';
import PageHeader from '../../components/PageHeader';
import SearchBar from '../../components/SearchBar';
import FilterBar, { FilterDate, FilterSelect } from '../../components/FilterBar';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import CurrencyText from '../../components/CurrencyText';
import ConfirmDialog from '../../components/ConfirmDialog';
import { ORDER_STATUS_OPTIONS, PAYMENT_STATUS_OPTIONS } from '../../utils/constants';
import { formatDate, formatNumber } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/errorMessage';
import SalesOrderViewDialog from './SalesOrderViewDialog';
import { downloadInvoice } from './invoice';

const INITIAL = {
  search: '',
  customerId: '',
  materialId: '',
  paymentStatus: '',
  status: '',
  pendingOnly: '',
  fromDate: '',
  toDate: '',
};

export default function SalesOrdersPage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const notify = useNotify();
  const table = useServerTable(salesOrderApi.list, { initialFilters: INITIAL, initialSort: { field: 'id', direction: 'desc' } });
  const customers = useCustomers(false);
  const materials = useMaterials();
  const [viewId, setViewId] = useState(null);
  const [cancelRow, setCancelRow] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const canCreate = can('salesOrders', 'create');
  const canUpdate = can('salesOrders', 'update');
  const canCancel = can('salesOrders', 'cancel');

  const handleCancel = async (reason) => {
    setCancelling(true);
    try {
      await salesOrderApi.cancel(cancelRow.id, reason?.trim() || undefined);
      notify.success(`Sales order ${cancelRow.salesOrderNumber} cancelled`);
      setCancelRow(null);
      table.refresh();
    } catch (err) {
      notify.error(getErrorMessage(err, 'Failed to cancel sales order'));
    } finally {
      setCancelling(false);
    }
  };

  const columns = [
    { key: 'salesOrderNumber', label: 'Order No.', sortable: true },
    { key: 'invoiceNumber', label: 'Invoice', sortable: true },
    { key: 'orderDate', label: 'Date', sortable: true, render: (r) => formatDate(r.orderDate) },
    { key: 'customerName', label: 'Customer' },
    { key: 'materialSummary', label: 'Materials', minWidth: 160 },
    { key: 'totalQuantity', label: 'Qty', align: 'right', render: (r) => formatNumber(r.totalQuantity) },
    { key: 'totalAmount', label: 'Total', align: 'right', sortable: true, render: (r) => <CurrencyText value={r.totalAmount} /> },
    { key: 'receivedAmount', label: 'Received', align: 'right', render: (r) => <CurrencyText value={r.receivedAmount} /> },
    {
      key: 'pendingAmount',
      label: 'Pending',
      align: 'right',
      render: (r) => <CurrencyText value={r.pendingAmount} color={Number(r.pendingAmount) > 0 ? 'error.main' : undefined} />,
    },
    { key: 'paymentStatus', label: 'Payment', render: (r) => <StatusChip status={r.paymentStatus} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusChip status={r.status} /> },
  ];

  return (
    <>
      <PageHeader title="Sales Orders" subtitle="Customer orders and invoices">
        {canCreate && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/sales-orders/new')}>
            New Sales Order
          </Button>
        )}
      </PageHeader>

      <FilterBar onReset={table.resetFilters}>
        <SearchBar
          value={table.filters.search}
          onChange={(v) => table.setFilter('search', v)}
          placeholder="Search order, invoice, customer..."
        />
        <FilterSelect
          label="Customer"
          value={table.filters.customerId}
          onChange={(v) => table.setFilter('customerId', v)}
          options={customers.options}
          loading={customers.loading}
          minWidth={200}
        />
        <FilterSelect
          label="Material"
          value={table.filters.materialId}
          onChange={(v) => table.setFilter('materialId', v)}
          options={materials.options}
          loading={materials.loading}
        />
        <FilterSelect
          label="Payment Status"
          value={table.filters.paymentStatus}
          onChange={(v) => table.setFilter('paymentStatus', v)}
          options={PAYMENT_STATUS_OPTIONS}
        />
        <FilterSelect
          label="Status"
          value={table.filters.status}
          onChange={(v) => table.setFilter('status', v)}
          options={ORDER_STATUS_OPTIONS}
          minWidth={130}
        />
        <FilterSelect
          label="Pending"
          value={table.filters.pendingOnly}
          onChange={(v) => table.setFilter('pendingOnly', v)}
          options={[{ value: 'true', label: 'Pending only' }]}
          minWidth={140}
        />
        <FilterDate label="From" value={table.filters.fromDate} onChange={(v) => table.setFilter('fromDate', v)} />
        <FilterDate label="To" value={table.filters.toDate} onChange={(v) => table.setFilter('toDate', v)} />
      </FilterBar>

      <DataTable
        columns={columns}
        table={table}
        emptyText="No sales orders found"
        rowSx={(r) => (r.status === 'CANCELLED' ? { '& td': { color: 'text.disabled' } } : undefined)}
        actions={(row) => {
          const active = row.status === 'ACTIVE';
          return (
            <>
              <Tooltip title="View">
                <IconButton aria-label="View sales order" size="small" onClick={() => setViewId(row.id)}>
                  <VisibilityIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Download invoice">
                <IconButton aria-label="Download invoice" size="small" color="secondary" onClick={() => downloadInvoice(row, notify)}>
                  <PictureAsPdfIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              {canUpdate && active && (
                <Tooltip title="Edit">
                  <IconButton aria-label="Edit sales order" size="small" color="primary" onClick={() => navigate(`/sales-orders/${row.id}/edit`)}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )}
              {canCancel && active && (
                <Tooltip title="Cancel">
                  <IconButton aria-label="Cancel sales order" size="small" color="error" onClick={() => setCancelRow(row)}>
                    <CancelIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )}
            </>
          );
        }}
      />

      <SalesOrderViewDialog id={viewId} onClose={() => setViewId(null)} />

      <ConfirmDialog
        open={!!cancelRow}
        title="Cancel Sales Order"
        message={
          <Typography component="span">
            Cancel sales order <b>{cancelRow?.salesOrderNumber}</b>? Stock will be restored.
          </Typography>
        }
        inputLabel="Reason (optional)"
        confirmText="Cancel Order"
        cancelText="Keep"
        color="error"
        loading={cancelling}
        onConfirm={handleCancel}
        onClose={() => setCancelRow(null)}
      />
    </>
  );
}

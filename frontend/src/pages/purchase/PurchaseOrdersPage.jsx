import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, IconButton, Tooltip, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import CancelIcon from '@mui/icons-material/Cancel';
import { purchaseOrderApi } from '../../api/purchaseOrderApi';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import useServerTable from '../../hooks/useServerTable';
import { useSuppliers } from '../../hooks/useLookups';
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
import PurchaseOrderViewDialog from './PurchaseOrderViewDialog';

const INITIAL = { search: '', supplierId: '', paymentStatus: '', status: '', fromDate: '', toDate: '' };

export default function PurchaseOrdersPage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const notify = useNotify();
  const table = useServerTable(purchaseOrderApi.list, { initialFilters: INITIAL, initialSort: { field: 'id', direction: 'desc' } });
  const suppliers = useSuppliers(false);
  const [viewId, setViewId] = useState(null);
  const [cancelRow, setCancelRow] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const canCreate = can('purchaseOrders', 'create');
  const canUpdate = can('purchaseOrders', 'update');
  const canCancel = can('purchaseOrders', 'cancel');

  const handleCancel = async (reason) => {
    setCancelling(true);
    try {
      await purchaseOrderApi.cancel(cancelRow.id, reason?.trim() || undefined);
      notify.success(`Purchase order ${cancelRow.poNumber} cancelled`);
      setCancelRow(null);
      table.refresh();
    } catch (err) {
      notify.error(getErrorMessage(err, 'Failed to cancel purchase order'));
    } finally {
      setCancelling(false);
    }
  };

  const columns = [
    { key: 'poNumber', label: 'PO No.', sortable: true },
    { key: 'poDate', label: 'Date', sortable: true, render: (r) => formatDate(r.poDate) },
    { key: 'supplierName', label: 'Supplier' },
    { key: 'materialSummary', label: 'Materials', minWidth: 160 },
    { key: 'totalQuantity', label: 'Qty', align: 'right', render: (r) => formatNumber(r.totalQuantity) },
    { key: 'totalAmount', label: 'Total', align: 'right', sortable: true, render: (r) => <CurrencyText value={r.totalAmount} /> },
    { key: 'paidAmount', label: 'Paid', align: 'right', render: (r) => <CurrencyText value={r.paidAmount} /> },
    { key: 'balanceAmount', label: 'Balance', align: 'right', render: (r) => <CurrencyText value={r.balanceAmount} /> },
    { key: 'paymentStatus', label: 'Payment', render: (r) => <StatusChip status={r.paymentStatus} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusChip status={r.status} /> },
  ];

  return (
    <>
      <PageHeader title="Purchase Orders" subtitle="Stone purchases from suppliers">
        {canCreate && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/purchase-orders/new')}>
            New Purchase Order
          </Button>
        )}
      </PageHeader>

      <FilterBar onReset={table.resetFilters}>
        <SearchBar
          value={table.filters.search}
          onChange={(v) => table.setFilter('search', v)}
          placeholder="Search PO no., vehicle, supplier..."
        />
        <FilterSelect
          label="Supplier"
          value={table.filters.supplierId}
          onChange={(v) => table.setFilter('supplierId', v)}
          options={suppliers.options}
          loading={suppliers.loading}
          minWidth={200}
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
        <FilterDate label="From" value={table.filters.fromDate} onChange={(v) => table.setFilter('fromDate', v)} />
        <FilterDate label="To" value={table.filters.toDate} onChange={(v) => table.setFilter('toDate', v)} />
      </FilterBar>

      <DataTable
        columns={columns}
        table={table}
        emptyText="No purchase orders found"
        rowSx={(r) => (r.status === 'CANCELLED' ? { '& td': { color: 'text.disabled' } } : undefined)}
        actions={(row) => {
          const active = row.status === 'ACTIVE';
          return (
            <>
              <Tooltip title="View">
                <IconButton aria-label="View purchase order" size="small" onClick={() => setViewId(row.id)}>
                  <VisibilityIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              {canUpdate && active && (
                <Tooltip title={Number(row.paidAmount) > 0 ? 'Cannot edit: payment exists' : 'Edit'}>
                  <span>
                    <IconButton
                      aria-label={Number(row.paidAmount) > 0 ? 'Cannot edit purchase order: payment exists' : 'Edit purchase order'}
                      size="small"
                      color="primary"
                      disabled={Number(row.paidAmount) > 0}
                      onClick={() => navigate(`/purchase-orders/${row.id}/edit`)}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                  </span>
                </Tooltip>
              )}
              {canCancel && active && (
                <Tooltip title={Number(row.paidAmount) > 0 ? 'Cannot cancel: payment exists' : 'Cancel'}>
                  <span>
                    <IconButton
                      aria-label={Number(row.paidAmount) > 0 ? 'Cannot cancel purchase order: payment exists' : 'Cancel purchase order'}
                      size="small"
                      color="error"
                      disabled={Number(row.paidAmount) > 0}
                      onClick={() => setCancelRow(row)}
                    >
                      <CancelIcon fontSize="small" />
                    </IconButton>
                  </span>
                </Tooltip>
              )}
            </>
          );
        }}
      />

      <PurchaseOrderViewDialog id={viewId} onClose={() => setViewId(null)} />

      <ConfirmDialog
        open={!!cancelRow}
        title="Cancel Purchase Order"
        message={
          <Typography component="span">
            Cancel purchase order <b>{cancelRow?.poNumber}</b>? Stock added by this order will be reversed.
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

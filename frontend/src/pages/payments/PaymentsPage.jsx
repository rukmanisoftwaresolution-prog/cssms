import { useEffect, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import useServerTable from '../../hooks/useServerTable';
import PageHeader from '../../components/PageHeader';
import SearchBar from '../../components/SearchBar';
import FilterBar, { FilterDate, FilterSelect } from '../../components/FilterBar';
import DataTable from '../../components/DataTable';
import ConfirmDialog from '../../components/ConfirmDialog';
import CurrencyText from '../../components/CurrencyText';
import { FormDatePicker, FormSelect, FormTextField } from '../../components/FormFields';
import { PAYMENT_MODE_OPTIONS } from '../../utils/constants';
import { formatCurrency, formatDate, humanize, today, toNumber } from '../../utils/formatters';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { combine, rules } from '../../utils/validators';

function PaymentDialog({ config, parties, onClose, onSaved }) {
  const notify = useNotify();
  const [docs, setDocs] = useState([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const {
    control,
    handleSubmit,
    setError,
    setValue,
    getValues,
    formState: { isSubmitting },
  } = useForm({
    defaultValues: {
      paymentDate: today(),
      [config.partyField]: '',
      [config.docField]: '',
      paymentMode: 'CASH',
      [config.amountField]: '',
      referenceNumber: '',
      remarks: '',
    },
  });
  const partyId = useWatch({ control, name: config.partyField });
  const docId = useWatch({ control, name: config.docField });
  const selectedDoc = docs.find((d) => d.id === Number(docId));
  const pending = selectedDoc ? config.pendingOf(selectedDoc) : null;

  useEffect(() => {
    setValue(config.docField, '');
    setValue(config.amountField, '');
    setDocs([]);
    if (!partyId) return undefined;
    let cancelled = false;
    setDocsLoading(true);
    config
      .loadPending(partyId)
      .then((res) => {
        if (cancelled) return;
        // pendingOnly is also filtered client-side in case the backend ignores it
        setDocs((res?.content || []).filter((d) => d.status !== 'CANCELLED' && config.pendingOf(d) > 0));
      })
      .catch((err) => !cancelled && notify.error(getErrorMessage(err, `Failed to load pending ${config.docPlural}`)))
      .finally(() => !cancelled && setDocsLoading(false));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [partyId]);

  const handleDocChangeEffect = selectedDoc?.id;
  useEffect(() => {
    if (selectedDoc) setValue(config.amountField, config.pendingOf(selectedDoc));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [handleDocChangeEffect]);

  const onSubmit = async (values) => {
    const payload = {
      paymentDate: values.paymentDate,
      [config.partyField]: Number(values[config.partyField]),
      [config.docField]: Number(values[config.docField]),
      paymentMode: values.paymentMode,
      [config.amountField]: Number(values[config.amountField]),
      referenceNumber: values.referenceNumber?.trim() || null,
      remarks: values.remarks?.trim() || null,
    };
    try {
      await config.api.create(payload);
      notify.success(`${config.singular} recorded successfully`);
      onSaved();
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  return (
    <Dialog open onClose={isSubmitting ? undefined : onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogTitle>{config.dialogTitle}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid item xs={12} sm={6}>
              <FormDatePicker name="paymentDate" control={control} label="Payment Date" required rules={rules.required('Payment date')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormSelect
                name="paymentMode"
                control={control}
                label="Payment Mode"
                required
                options={PAYMENT_MODE_OPTIONS}
                rules={rules.required('Payment mode')}
              />
            </Grid>
            <Grid item xs={12}>
              <FormSelect
                name={config.partyField}
                control={control}
                label={config.partyLabel}
                required
                options={parties.options}
                loading={parties.loading}
                rules={rules.required(config.partyLabel)}
              />
            </Grid>
            <Grid item xs={12}>
              <FormSelect
                name={config.docField}
                control={control}
                label={config.docLabel}
                required
                options={docs.map((d) => ({ value: d.id, label: config.docOptionLabel(d) }))}
                loading={docsLoading}
                disabled={!partyId}
                rules={rules.required(config.docLabel)}
                helperText={
                  partyId && !docsLoading && docs.length === 0 ? `No pending ${config.docPlural} for this ${config.partyLabel.toLowerCase()}` : undefined
                }
              />
            </Grid>
            {selectedDoc && (
              <Grid item xs={12}>
                <Alert severity="info" icon={false} sx={{ py: 0.5 }}>
                  Pending amount: <b>{formatCurrency(pending)}</b>
                </Alert>
              </Grid>
            )}
            <Grid item xs={12} sm={6}>
              <FormTextField
                name={config.amountField}
                control={control}
                type="number"
                label="Amount"
                required
                rules={combine(rules.positive('Amount'), {
                  validate: (v) => pending === null || toNumber(v) <= pending || `Cannot exceed pending ${formatCurrency(pending)}`,
                })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormTextField name="referenceNumber" control={control} label="Reference / Cheque No." />
            </Grid>
            <Grid item xs={12}>
              <FormTextField name="remarks" control={control} label="Remarks" multiline minRows={2} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : config.submitLabel}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

/** Shared payments screen (customer receipts / supplier payments). See Customer/SupplierPaymentsPage for config. */
export default function PaymentsPage({ config }) {
  const { can } = useAuth();
  const notify = useNotify();
  const table = useServerTable(config.api.list, {
    initialFilters: { search: '', [config.partyField]: '', paymentMode: '', fromDate: '', toDate: '' },
    initialSort: { field: 'id', direction: 'desc' },
  });
  const parties = config.useParties();
  const [open, setOpen] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const canCreate = can(config.permission, 'create');
  const canDelete = can(config.permission, 'delete');

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await config.api.remove(toDelete.id);
      notify.success(`${config.singular} deleted and reversed`);
      setToDelete(null);
      table.refresh();
    } catch (err) {
      notify.error(getErrorMessage(err, 'Delete failed'));
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    { key: 'paymentDate', label: 'Date', sortable: true, render: (r) => formatDate(r.paymentDate) },
    { key: config.partyNameField, label: config.partyLabel },
    { key: config.docNumberField, label: config.docLabel },
    { key: 'paymentMode', label: 'Mode', render: (r) => humanize(r.paymentMode) },
    {
      key: config.amountField,
      label: 'Amount',
      align: 'right',
      sortable: true,
      render: (r) => <CurrencyText value={r[config.amountField]} />,
    },
    { key: 'referenceNumber', label: 'Reference' },
    { key: 'remarks', label: 'Remarks' },
  ];

  return (
    <>
      <PageHeader title={config.title} subtitle={config.subtitle}>
        {canCreate && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
            {config.addLabel}
          </Button>
        )}
      </PageHeader>

      <FilterBar onReset={table.resetFilters}>
        <SearchBar value={table.filters.search} onChange={(v) => table.setFilter('search', v)} placeholder="Search..." />
        <FilterSelect
          label={config.partyLabel}
          value={table.filters[config.partyField]}
          onChange={(v) => table.setFilter(config.partyField, v)}
          options={parties.options}
          loading={parties.loading}
          minWidth={210}
        />
        <FilterSelect
          label="Payment Mode"
          value={table.filters.paymentMode}
          onChange={(v) => table.setFilter('paymentMode', v)}
          options={PAYMENT_MODE_OPTIONS}
        />
        <FilterDate label="From" value={table.filters.fromDate} onChange={(v) => table.setFilter('fromDate', v)} />
        <FilterDate label="To" value={table.filters.toDate} onChange={(v) => table.setFilter('toDate', v)} />
      </FilterBar>

      <DataTable
        columns={columns}
        table={table}
        emptyText={`No ${config.title.toLowerCase()} found`}
        actions={
          canDelete
            ? (row) => (
                <Tooltip title="Delete (reverses the payment)">
                  <IconButton aria-label="Delete payment and reverse balance" size="small" color="error" onClick={() => setToDelete(row)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )
            : undefined
        }
      />

      {open && (
        <PaymentDialog
          config={config}
          parties={parties}
          onClose={() => setOpen(false)}
          onSaved={() => {
            setOpen(false);
            table.refresh();
          }}
        />
      )}

      <ConfirmDialog
        open={!!toDelete}
        title={`Delete ${config.singular}`}
        message={`Delete this payment of ${formatCurrency(toDelete?.[config.amountField])}? The amount will be reversed on the ${config.docLabel.toLowerCase()}.`}
        confirmText="Delete"
        color="error"
        loading={deleting}
        onConfirm={handleDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}

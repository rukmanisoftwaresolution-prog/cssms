import { useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DownloadIcon from '@mui/icons-material/Download';
import { expenseApi } from '../../api/expenseApi';
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
import { EXPENSE_TYPE_OPTIONS, MAX_ATTACHMENT_BYTES } from '../../utils/constants';
import { formatDate, humanize, today } from '../../utils/formatters';
import { saveResponse } from '../../utils/download';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { rules } from '../../utils/validators';

async function downloadAttachment(expense, notify) {
  try {
    const res = await expenseApi.downloadAttachment(expense.id);
    saveResponse(res, expense.attachmentName || `expense-${expense.id}-attachment`);
  } catch (err) {
    notify.error(getErrorMessage(err, 'Failed to download attachment'));
  }
}

function ExpenseDialog({ row, onClose, onSaved }) {
  const notify = useNotify();
  const isEdit = !!row;
  const fileRef = useRef(null);
  const [file, setFile] = useState(null);
  const [existing, setExisting] = useState(row?.hasAttachment ? row.attachmentName || 'attachment' : null);
  const [removing, setRemoving] = useState(false);
  const [removedAny, setRemovedAny] = useState(false);
  const closeDialog = () => (removedAny ? onSaved() : onClose());
  const {
    control,
    handleSubmit,
    setError,
    formState: { isSubmitting },
  } = useForm({
    defaultValues: {
      expenseDate: row?.expenseDate || today(),
      expenseType: row?.expenseType || '',
      amount: row?.amount ?? '',
      description: row?.description || '',
    },
  });

  const pickFile = (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f) return;
    if (f.size > MAX_ATTACHMENT_BYTES) {
      notify.error('Attachment must be 5 MB or smaller');
      return;
    }
    setFile(f);
  };

  const removeExisting = async () => {
    setRemoving(true);
    try {
      await expenseApi.removeAttachment(row.id);
      setExisting(null);
      setRemovedAny(true);
      notify.success('Attachment removed');
    } catch (err) {
      notify.error(getErrorMessage(err, 'Failed to remove attachment'));
    } finally {
      setRemoving(false);
    }
  };

  const onSubmit = async (values) => {
    const payload = {
      expenseDate: values.expenseDate,
      expenseType: values.expenseType,
      amount: Number(values.amount),
      description: values.description?.trim() || null,
    };
    let saved;
    try {
      saved = isEdit ? await expenseApi.update(row.id, payload) : await expenseApi.create(payload);
    } catch (err) {
      handleFormError(err, { setError, notify });
      return;
    }
    if (file) {
      try {
        await expenseApi.uploadAttachment(saved?.id ?? row.id, file);
      } catch (err) {
        notify.error(`Expense saved, but attachment upload failed: ${getErrorMessage(err)}`);
        onSaved();
        return;
      }
    }
    notify.success(`Expense ${isEdit ? 'updated' : 'created'} successfully`);
    onSaved();
  };

  return (
    <Dialog open onClose={isSubmitting ? undefined : closeDialog} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogTitle>{isEdit ? 'Edit' : 'Add'} Expense</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid item xs={12} sm={6}>
              <FormDatePicker name="expenseDate" control={control} label="Date" required rules={rules.required('Date')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormSelect
                name="expenseType"
                control={control}
                label="Expense Type"
                required
                options={EXPENSE_TYPE_OPTIONS}
                rules={rules.required('Expense type')}
              />
            </Grid>
            <Grid item xs={12}>
              <FormTextField name="amount" control={control} type="number" label="Amount" required rules={rules.positive('Amount')} />
            </Grid>
            <Grid item xs={12}>
              <FormTextField name="description" control={control} label="Description" multiline minRows={2} />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle2" gutterBottom>
                Attachment (max 5 MB)
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                <input ref={fileRef} type="file" hidden onChange={pickFile} />
                <Button variant="outlined" startIcon={<AttachFileIcon />} onClick={() => fileRef.current?.click()}>
                  {file || existing ? 'Replace file' : 'Choose file'}
                </Button>
                {file && <Chip label={file.name} onDelete={() => setFile(null)} />}
                {!file && existing && (
                  <>
                    <Chip label={existing} variant="outlined" />
                    <Tooltip title="Download">
                      <IconButton aria-label="Download attachment" size="small" onClick={() => downloadAttachment(row, notify)}>
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Remove attachment">
                      <span>
                        <IconButton aria-label="Remove attachment" size="small" color="error" disabled={removing} onClick={removeExisting}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </>
                )}
              </Box>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={closeDialog} color="inherit" disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default function ExpensesPage() {
  const { can } = useAuth();
  const notify = useNotify();
  const table = useServerTable(expenseApi.list, {
    initialFilters: { search: '', expenseType: '', fromDate: '', toDate: '' },
    initialSort: { field: 'expenseDate', direction: 'desc' },
  });
  const [dialog, setDialog] = useState({ open: false, row: null });
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const canCreate = can('expenses', 'create');
  const canUpdate = can('expenses', 'update');
  const canDelete = can('expenses', 'delete');

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await expenseApi.remove(toDelete.id);
      notify.success('Expense deleted');
      setToDelete(null);
      table.refresh();
    } catch (err) {
      notify.error(getErrorMessage(err, 'Delete failed'));
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    { key: 'expenseDate', label: 'Date', sortable: true, render: (r) => formatDate(r.expenseDate) },
    { key: 'expenseType', label: 'Type', sortable: true, render: (r) => humanize(r.expenseType) },
    { key: 'description', label: 'Description', minWidth: 200 },
    { key: 'amount', label: 'Amount', align: 'right', sortable: true, render: (r) => <CurrencyText value={r.amount} /> },
    {
      key: 'attachment',
      label: 'Attachment',
      render: (r) =>
        r.hasAttachment ? (
          <Tooltip title={r.attachmentName || 'Download'}>
            <Chip
              size="small"
              icon={<AttachFileIcon />}
              label="Download"
              clickable
              variant="outlined"
              onClick={() => downloadAttachment(r, notify)}
            />
          </Tooltip>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <>
      <PageHeader title="Expenses" subtitle="Operational expenses">
        {canCreate && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true, row: null })}>
            Add Expense
          </Button>
        )}
      </PageHeader>

      <FilterBar onReset={table.resetFilters}>
        <SearchBar value={table.filters.search} onChange={(v) => table.setFilter('search', v)} placeholder="Search description..." />
        <FilterSelect
          label="Expense Type"
          value={table.filters.expenseType}
          onChange={(v) => table.setFilter('expenseType', v)}
          options={EXPENSE_TYPE_OPTIONS}
          minWidth={200}
        />
        <FilterDate label="From" value={table.filters.fromDate} onChange={(v) => table.setFilter('fromDate', v)} />
        <FilterDate label="To" value={table.filters.toDate} onChange={(v) => table.setFilter('toDate', v)} />
      </FilterBar>

      <DataTable
        columns={columns}
        table={table}
        emptyText="No expenses found"
        actions={
          canUpdate || canDelete
            ? (row) => (
                <>
                  {canUpdate && (
                    <Tooltip title="Edit">
                      <IconButton aria-label="Edit expense" size="small" color="primary" onClick={() => setDialog({ open: true, row })}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                  {canDelete && (
                    <Tooltip title="Delete">
                      <IconButton aria-label="Delete expense" size="small" color="error" onClick={() => setToDelete(row)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                </>
              )
            : undefined
        }
      />

      {dialog.open && (
        <ExpenseDialog
          row={dialog.row}
          onClose={() => setDialog({ open: false, row: null })}
          onSaved={() => {
            setDialog({ open: false, row: null });
            table.refresh();
          }}
        />
      )}

      <ConfirmDialog
        open={!!toDelete}
        title="Delete Expense"
        message="Are you sure you want to delete this expense?"
        confirmText="Delete"
        color="error"
        loading={deleting}
        onConfirm={handleDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}

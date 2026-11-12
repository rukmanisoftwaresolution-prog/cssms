import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
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
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import useServerTable from '../../hooks/useServerTable';
import PageHeader from '../../components/PageHeader';
import SearchBar from '../../components/SearchBar';
import FilterBar, { FilterSelect } from '../../components/FilterBar';
import DataTable from '../../components/DataTable';
import ConfirmDialog from '../../components/ConfirmDialog';
import { FormSelect, FormSwitch, FormTextField } from '../../components/FormFields';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { STATUS_OPTIONS } from '../../utils/constants';

const isBlank = (v) => v === undefined || v === null || String(v).trim() === '';

function buildDefaults(fields, row) {
  const values = {};
  fields.forEach((f) => {
    if (row) {
      values[f.name] = f.type === 'switch' ? !!row[f.name] : (row[f.name] ?? '');
      if (f.omitOnEdit) values[f.name] = '';
    } else {
      values[f.name] = f.default !== undefined ? f.default : f.type === 'switch' ? true : '';
    }
  });
  return values;
}

function buildPayload(fields, values, isEdit) {
  const payload = {};
  fields.forEach((f) => {
    let v = values[f.name];
    if (f.type === 'switch') {
      payload[f.name] = !!v;
      return;
    }
    if (isBlank(v)) {
      if (f.omitIfBlank && isEdit) return;
      payload[f.name] = f.required ? '' : null;
      return;
    }
    if (f.type === 'number') v = Number(v);
    else if (f.type !== 'password') v = String(v).trim();
    if (f.upper && typeof v === 'string') v = v.toUpperCase();
    payload[f.name] = v;
  });
  return payload;
}

function FormDialog({ open, config, row, onClose, onSaved }) {
  const notify = useNotify();
  const isEdit = !!row;
  const fields = config.fields;
  const {
    control,
    handleSubmit,
    setError,
    formState: { isSubmitting },
  } = useForm({ defaultValues: buildDefaults(fields, row) });

  const onSubmit = async (values) => {
    const payload = buildPayload(fields, values, isEdit);
    try {
      if (isEdit) await config.api.update(row.id, payload);
      else await config.api.create(payload);
      notify.success(`${config.singular} ${isEdit ? 'updated' : 'created'} successfully`);
      onSaved();
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  const renderField = (f) => {
    const rules = typeof f.rules === 'function' ? f.rules(isEdit) : f.rules;
    const common = {
      name: f.name,
      control,
      label: f.label,
      rules,
      required: typeof f.required === 'function' ? f.required(isEdit) : !!f.required,
      disabled: f.disabledOnEdit && isEdit,
    };
    if (f.type === 'switch') return <FormSwitch name={f.name} control={control} label={f.label} />;
    if (f.type === 'select') return <FormSelect {...common} options={f.options} />;
    return (
      <FormTextField
        {...common}
        type={f.type === 'textarea' ? 'text' : f.type || 'text'}
        multiline={f.type === 'textarea'}
        minRows={f.type === 'textarea' ? 2 : undefined}
        helperText={f.helperText}
        autoComplete="off"
      />
    );
  };

  return (
    <Dialog open={open} onClose={isSubmitting ? undefined : onClose} maxWidth="md" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogTitle>
          {isEdit ? 'Edit' : 'Add'} {config.singular}
        </DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 1 }}>
            {fields.map((f) => (
              <Grid item xs={12} sm={f.full || f.type === 'textarea' ? 12 : 6} key={f.name}>
                {renderField(f)}
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={isSubmitting}>
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

/**
 * Generic list + add/edit dialog + delete page used by master data screens.
 * See pages/masters/*Page.jsx for the config shape.
 */
export default function MasterCrudPage({ config }) {
  const { can } = useAuth();
  const notify = useNotify();
  const table = useServerTable(config.api.list, {
    initialFilters: { search: '', ...(config.hasStatus ? { status: '' } : {}), ...(config.extraFilter ? { [config.extraFilter.name]: '' } : {}) },
    initialSort: config.initialSort || null,
  });
  const [dialog, setDialog] = useState({ open: false, row: null });
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const canCreate = can(config.permission, 'create');
  const canUpdate = can(config.permission, 'update');
  const canDelete = can(config.permission, 'delete');

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await config.api.remove(toDelete.id);
      notify.success(`${config.singular} deleted successfully`);
      setToDelete(null);
      table.refresh();
    } catch (err) {
      notify.error(getErrorMessage(err, 'Delete failed'));
    } finally {
      setDeleting(false);
    }
  };

  const hasActions = canUpdate || canDelete;
  const columns = useMemo(() => config.columns, [config.columns]);

  return (
    <>
      <PageHeader title={config.title} subtitle={config.subtitle}>
        {canCreate && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true, row: null })}>
            Add {config.singular}
          </Button>
        )}
      </PageHeader>

      <FilterBar onReset={table.resetFilters}>
        <SearchBar
          value={table.filters.search}
          onChange={(v) => table.setFilter('search', v)}
          placeholder={config.searchPlaceholder || 'Search...'}
        />
        {config.hasStatus && (
          <FilterSelect
            label="Status"
            value={table.filters.status}
            onChange={(v) => table.setFilter('status', v)}
            options={STATUS_OPTIONS}
          />
        )}
        {config.extraFilter && (
          <FilterSelect
            label={config.extraFilter.label}
            value={table.filters[config.extraFilter.name]}
            onChange={(v) => table.setFilter(config.extraFilter.name, v)}
            options={config.extraFilter.options}
          />
        )}
      </FilterBar>

      <DataTable
        columns={columns}
        table={table}
        actions={
          hasActions
            ? (row) => (
                <>
                  {canUpdate && (
                    <Tooltip title="Edit">
                      <IconButton aria-label={`Edit ${config.singular}`} size="small" color="primary" onClick={() => setDialog({ open: true, row })}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                  {canDelete && (
                    <Tooltip title="Delete">
                      <IconButton aria-label={`Delete ${config.singular}`} size="small" color="error" onClick={() => setToDelete(row)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                </>
              )
            : undefined
        }
        emptyText={`No ${config.title.toLowerCase()} found`}
      />

      {dialog.open && (
        <FormDialog
          open
          config={config}
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
        title={`Delete ${config.singular}`}
        message={`Are you sure you want to delete "${toDelete ? config.rowLabel(toDelete) : ''}"? This action cannot be undone.`}
        confirmText="Delete"
        color="error"
        loading={deleting}
        onConfirm={handleDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}

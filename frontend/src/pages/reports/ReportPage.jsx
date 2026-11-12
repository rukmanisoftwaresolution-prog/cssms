import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Box, Button, Grid, Paper } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { reportApi } from '../../api/reportApi';
import { useNotify } from '../../context/NotificationContext';
import { useCustomers, useMaterials, useSuppliers } from '../../hooks/useLookups';
import PageHeader from '../../components/PageHeader';
import ReportTable from '../../components/ReportTable';
import { FormDatePicker, FormSelect, FormTextField } from '../../components/FormFields';
import { EXPENSE_TYPE_OPTIONS } from '../../utils/constants';
import { getErrorMessage } from '../../utils/errorMessage';
import { rules } from '../../utils/validators';

/**
 * Generic report screen.
 * filters: [{ name, label, type: 'date' | 'text' | 'select', lookup?: 'customers'|'suppliers'|'materials'|'expenseTypes', required? }]
 * Reports without required filters load automatically.
 */
export default function ReportPage({ reportKey, title, subtitle, filters = [] }) {
  const notify = useNotify();
  const needs = (lookup) => filters.some((f) => f.lookup === lookup);
  const customers = useCustomers(false, needs('customers'));
  const suppliers = useSuppliers(false, needs('suppliers'));
  const materials = useMaterials(needs('materials'));
  const lookups = {
    customers: customers,
    suppliers: suppliers,
    materials: materials,
    expenseTypes: { options: EXPENSE_TYPE_OPTIONS, loading: false },
  };

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [applied, setApplied] = useState({});
  const requestId = useRef(0);

  const {
    control,
    handleSubmit,
    getValues,
    reset,
  } = useForm({ defaultValues: Object.fromEntries(filters.map((f) => [f.name, ''])) });

  const run = useCallback(
    async (values) => {
      const params = Object.fromEntries(Object.entries(values).filter(([, v]) => v !== '' && v != null));
      const id = ++requestId.current;
      setLoading(true);
      try {
        const data = await reportApi.get(reportKey, params);
        if (id !== requestId.current) return;
        setReport(data);
        setApplied(params);
      } catch (err) {
        if (id === requestId.current) {
          setReport(null);
          notify.error(getErrorMessage(err, 'Failed to generate report'));
        }
      } finally {
        if (id === requestId.current) setLoading(false);
      }
    },
    [reportKey, notify]
  );

  // reset when switching report routes (same component instance reused by the router)
  useEffect(() => {
    setReport(null);
    setApplied({});
    reset(Object.fromEntries(filters.map((f) => [f.name, ''])));
    if (!filters.some((f) => f.required)) run({});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportKey]);

  const dateRange = (name) => ({
    validate: (v) => {
      if (!v) return true;
      if (name === 'toDate') {
        const from = getValues('fromDate');
        return !from || v >= from || 'To date must be on or after From date';
      }
      return true;
    },
  });

  return (
    <>
      <PageHeader title={title} subtitle={subtitle} />
      {filters.length > 0 && (
        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2 }, mb: 2, minWidth: 0 }}>
          <form onSubmit={handleSubmit(run)} noValidate>
            <Grid container spacing={2} alignItems="flex-start">
              {filters.map((f) => (
                <Grid item xs={12} sm={6} md={3} key={f.name}>
                  {f.type === 'date' ? (
                    <FormDatePicker name={f.name} control={control} label={f.label} rules={dateRange(f.name)} />
                  ) : f.type === 'select' ? (
                    <FormSelect
                      name={f.name}
                      control={control}
                      label={f.label}
                      required={f.required}
                      emptyLabel={f.required ? undefined : 'All'}
                      options={lookups[f.lookup]?.options || f.options || []}
                      loading={lookups[f.lookup]?.loading}
                      rules={f.required ? rules.required(f.label) : undefined}
                    />
                  ) : (
                    <FormTextField name={f.name} control={control} label={f.label} />
                  )}
                </Grid>
              ))}
              <Grid item xs={12} sm={6} md={3}>
                <Box
                  sx={{
                    display: 'flex',
                    flexDirection: { xs: 'column', sm: 'row' },
                    gap: 1,
                    width: '100%',
                    '& .MuiButton-root': { width: { xs: '100%', sm: 'auto' }, minHeight: 44 },
                  }}
                >
                  <Button type="submit" variant="contained" startIcon={<PlayArrowIcon />} disabled={loading}>
                    Generate
                  </Button>
                  <Button
                    color="inherit"
                    onClick={() => {
                      reset(Object.fromEntries(filters.map((f) => [f.name, ''])));
                      setReport(null);
                      if (!filters.some((f) => f.required)) run({});
                    }}
                  >
                    Reset
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </form>
        </Paper>
      )}
      <ReportTable report={report} loading={loading} exportKey={reportKey} exportParams={applied} />
    </>
  );
}

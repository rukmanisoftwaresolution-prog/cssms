import { useState } from 'react';
import { Box, Button, Grid, MenuItem, Paper, TextField } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { reportApi } from '../../api/reportApi';
import { useNotify } from '../../context/NotificationContext';
import PageHeader from '../../components/PageHeader';
import ReportTable from '../../components/ReportTable';
import { getErrorMessage } from '../../utils/errorMessage';
import { toISODate, today } from '../../utils/formatters';

const PERIODS = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'YEARLY', label: 'Yearly' },
  { value: 'CUSTOM', label: 'Custom' },
];

const thisMonth = () => today().slice(0, 7);

/** Maps the period selector into an inclusive fromDate/toDate range */
export function computeRange(period, { day, month, year, from, to }) {
  switch (period) {
    case 'DAILY':
      return day ? { fromDate: day, toDate: day } : null;
    case 'MONTHLY': {
      if (!/^\d{4}-\d{2}$/.test(month || '')) return null;
      const [y, m] = month.split('-').map(Number);
      return { fromDate: toISODate(new Date(y, m - 1, 1)), toDate: toISODate(new Date(y, m, 0)) };
    }
    case 'YEARLY': {
      const y = Number(year);
      if (!Number.isInteger(y) || y < 1900 || y > 2200) return null;
      return { fromDate: `${y}-01-01`, toDate: `${y}-12-31` };
    }
    default:
      return from && to ? { fromDate: from, toDate: to } : null;
  }
}

export default function ProfitLossPage() {
  const notify = useNotify();
  const [period, setPeriod] = useState('MONTHLY');
  const [vals, setVals] = useState({
    day: today(),
    month: thisMonth(),
    year: String(new Date().getFullYear()),
    from: '',
    to: '',
  });
  const [error, setError] = useState('');
  const [report, setReport] = useState(null);
  const [applied, setApplied] = useState({});
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setVals((v) => ({ ...v, [k]: e.target.value }));

  const generate = async (e) => {
    e?.preventDefault();
    const range = computeRange(period, vals);
    if (!range) {
      setError(period === 'CUSTOM' ? 'Select both From and To dates' : 'Select a valid period');
      return;
    }
    if (range.fromDate > range.toDate) {
      setError('From date must be on or before To date');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const data = await reportApi.get('profit-loss', range);
      setReport(data);
      setApplied(range);
    } catch (err) {
      setReport(null);
      notify.error(getErrorMessage(err, 'Failed to generate report'));
    } finally {
      setLoading(false);
    }
  };

  const errProps = error ? { error: true, helperText: error } : {};

  return (
    <>
      <PageHeader title="Profit & Loss" subtitle="Income vs expenses for the selected period" />
      <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2 }, mb: 2, minWidth: 0 }}>
        <form onSubmit={generate} noValidate>
          <Grid container spacing={2} alignItems="flex-start">
            <Grid item xs={12} sm={6} md={3}>
              <TextField select label="Period" value={period} onChange={(e) => { setPeriod(e.target.value); setError(''); }}>
                {PERIODS.map((p) => (
                  <MenuItem key={p.value} value={p.value}>
                    {p.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {period === 'DAILY' && (
              <Grid item xs={12} sm={6} md={3}>
                <TextField type="date" label="Date" value={vals.day} onChange={set('day')} InputLabelProps={{ shrink: true }} {...errProps} />
              </Grid>
            )}
            {period === 'MONTHLY' && (
              <Grid item xs={12} sm={6} md={3}>
                <TextField type="month" label="Month" value={vals.month} onChange={set('month')} InputLabelProps={{ shrink: true }} {...errProps} />
              </Grid>
            )}
            {period === 'YEARLY' && (
              <Grid item xs={12} sm={6} md={3}>
                <TextField type="number" label="Year" value={vals.year} onChange={set('year')} inputProps={{ min: 1900, max: 2200 }} {...errProps} />
              </Grid>
            )}
            {period === 'CUSTOM' && (
              <>
                <Grid item xs={12} sm={6} md={3}>
                  <TextField type="date" label="From Date" value={vals.from} onChange={set('from')} InputLabelProps={{ shrink: true }} {...errProps} />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <TextField type="date" label="To Date" value={vals.to} onChange={set('to')} InputLabelProps={{ shrink: true }} />
                </Grid>
              </>
            )}
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<PlayArrowIcon />}
                  disabled={loading}
                  sx={{ minHeight: 44, width: { xs: '100%', sm: 'auto' } }}
                >
                  Generate
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
      <ReportTable report={report} loading={loading} exportKey="profit-loss" exportParams={applied} />
    </>
  );
}

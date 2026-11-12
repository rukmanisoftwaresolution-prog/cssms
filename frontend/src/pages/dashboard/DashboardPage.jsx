import { useEffect, useState } from 'react';
import { Grid, Typography } from '@mui/material';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import PaymentsIcon from '@mui/icons-material/Payments';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import CallReceivedIcon from '@mui/icons-material/CallReceived';
import CallMadeIcon from '@mui/icons-material/CallMade';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import { dashboardApi } from '../../api/dashboardApi';
import { useNotify } from '../../context/NotificationContext';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import ChartCard from '../../components/ChartCard';
import { formatCompact, formatCurrency, formatNumber } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/errorMessage';

const PIE_COLORS = ['#1b4965', '#e07a1f', '#2e7d32', '#8e24aa', '#00838f', '#c62828', '#5d4037', '#546e7a'];

const CARDS = [
  { key: 'todaysPurchase', title: "Today's Purchase", icon: <ShoppingCartIcon />, color: 'primary' },
  { key: 'todaysSales', title: "Today's Sales", icon: <PointOfSaleIcon />, color: 'success' },
  { key: 'todaysCollection', title: "Today's Collection", icon: <PaymentsIcon />, color: 'success' },
  { key: 'todaysSupplierPayment', title: "Today's Supplier Payment", icon: <AccountBalanceWalletIcon />, color: 'warning' },
  { key: 'currentInventory', title: 'Current Inventory', icon: <Inventory2Icon />, color: 'info', format: 'number' },
  { key: 'receivables', title: 'Receivables', icon: <CallReceivedIcon />, color: 'secondary' },
  { key: 'payables', title: 'Payables', icon: <CallMadeIcon />, color: 'error' },
  { key: 'monthlyProfit', title: 'Monthly Profit', icon: <TrendingUpIcon />, color: 'success' },
  { key: 'monthlyLoss', title: 'Monthly Loss', icon: <TrendingDownIcon />, color: 'error' },
];

const axisTick = { fontSize: 12 };
const tooltipFmt = (v) => formatCurrency(v);

function TrendLine({ data, color, name }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="label" tick={axisTick} />
        <YAxis tick={axisTick} tickFormatter={formatCompact} width={60} />
        <Tooltip formatter={tooltipFmt} />
        <Line type="monotone" dataKey="value" name={name} stroke={color} strokeWidth={2.5} dot={{ r: 3 }} />
      </LineChart>
    </ResponsiveContainer>
  );
}

function SimpleBar({ data, color, name, layout = 'horizontal' }) {
  const vertical = layout === 'vertical';
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} layout={layout} margin={{ top: 8, right: 16, left: vertical ? 16 : 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        {vertical ? (
          <>
            <XAxis type="number" tick={axisTick} tickFormatter={formatCompact} />
            <YAxis type="category" dataKey="label" tick={axisTick} width={100} />
          </>
        ) : (
          <>
            <XAxis dataKey="label" tick={axisTick} />
            <YAxis tick={axisTick} tickFormatter={formatCompact} width={60} />
          </>
        )}
        <Tooltip formatter={tooltipFmt} />
        <Bar dataKey="value" name={name} fill={color} radius={[4, 4, 0, 0]}>
          {name === 'Profit' && data.map((d, i) => <Cell key={i} fill={Number(d.value) < 0 ? '#c62828' : color} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

export default function DashboardPage() {
  const notify = useNotify();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    dashboardApi
      .get()
      .then((d) => !cancelled && setData(d))
      .catch((err) => !cancelled && notify.error(getErrorMessage(err, 'Failed to load dashboard')))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [notify]);

  const list = (k) => data?.[k] || [];
  const isEmpty = (k) => list(k).length === 0;

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Business overview" />
      <Grid container spacing={2.5}>
        {CARDS.map((c) => (
          <Grid item xs={12} sm={6} md={3} xl={2} key={c.key}>
            <StatCard
              title={c.title}
              value={data?.cards?.[c.key]}
              icon={c.icon}
              color={c.color}
              format={c.format || 'currency'}
              loading={loading}
            />
          </Grid>
        ))}
      </Grid>

      <Typography variant="h6" sx={{ mt: 4, mb: 2 }}>
        Trends (last 6 months)
      </Typography>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Purchase Trend" loading={loading} empty={isEmpty('purchaseTrend')}>
            <TrendLine data={list('purchaseTrend')} color="#1b4965" name="Purchase" />
          </ChartCard>
        </Grid>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Sales Trend" loading={loading} empty={isEmpty('salesTrend')}>
            <TrendLine data={list('salesTrend')} color="#2e7d32" name="Sales" />
          </ChartCard>
        </Grid>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Profit Trend" loading={loading} empty={isEmpty('profitTrend')}>
            <SimpleBar data={list('profitTrend')} color="#e07a1f" name="Profit" />
          </ChartCard>
        </Grid>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Material-wise Sales" loading={loading} empty={isEmpty('materialWiseSales')}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={list('materialWiseSales')} dataKey="value" nameKey="label" outerRadius="75%" label={(e) => e.label}>
                  {list('materialWiseSales').map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatNumber(v)} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Top Customers" loading={loading} empty={isEmpty('topCustomers')}>
            <SimpleBar data={list('topCustomers')} color="#1b4965" name="Sales" layout="vertical" />
          </ChartCard>
        </Grid>
        <Grid item xs={12} sm={6}>
          <ChartCard title="Top Suppliers" loading={loading} empty={isEmpty('topSuppliers')}>
            <SimpleBar data={list('topSuppliers')} color="#8e24aa" name="Purchase" layout="vertical" />
          </ChartCard>
        </Grid>
      </Grid>
    </>
  );
}

import { useEffect, useState } from 'react';
import {
  alpha,
  Box,
  Card,
  CardContent,
  Chip,
  Grid,
  LinearProgress,
  Paper,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import ViewModuleIcon from '@mui/icons-material/ViewModule';
import TableRowsIcon from '@mui/icons-material/TableRows';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { inventoryApi } from '../../api/inventoryApi';
import { useNotify } from '../../context/NotificationContext';
import PageHeader from '../../components/PageHeader';
import SearchBar from '../../components/SearchBar';
import FilterBar from '../../components/FilterBar';
import { LOW_STOCK_THRESHOLD } from '../../utils/constants';
import { formatNumber } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/errorMessage';

const stockState = (qty) => {
  const n = Number(qty) || 0;
  if (n <= 0) return { color: 'error', label: 'Out of stock' };
  if (n <= LOW_STOCK_THRESHOLD) return { color: 'warning', label: 'Low stock' };
  return { color: 'success', label: 'In stock' };
};

export default function InventoryPage() {
  const notify = useNotify();
  const [search, setSearch] = useState('');
  const [view, setView] = useState('cards');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    inventoryApi
      .list({ search })
      .then((list) => !cancelled && setItems(Array.isArray(list) ? list : []))
      .catch((err) => !cancelled && notify.error(getErrorMessage(err, 'Failed to load inventory')))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [search, notify]);

  return (
    <>
      <PageHeader title="Inventory" subtitle={`Low stock threshold: ${LOW_STOCK_THRESHOLD} units`}>
        <ToggleButtonGroup size="small" exclusive value={view} onChange={(_, v) => v && setView(v)}>
          <ToggleButton value="cards" aria-label="card view">
            <ViewModuleIcon fontSize="small" />
          </ToggleButton>
          <ToggleButton value="table" aria-label="table view">
            <TableRowsIcon fontSize="small" />
          </ToggleButton>
        </ToggleButtonGroup>
      </PageHeader>

      <FilterBar>
        <SearchBar value={search} onChange={setSearch} placeholder="Search material..." />
      </FilterBar>

      {loading ? (
        <Grid container spacing={2}>
          {[1, 2, 3, 4].map((i) => (
            <Grid item xs={12} sm={6} md={3} xl={2} key={i}>
              <Skeleton variant="rounded" height={170} />
            </Grid>
          ))}
        </Grid>
      ) : items.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 6, textAlign: 'center', color: 'text.secondary' }}>
          No inventory records found
        </Paper>
      ) : view === 'cards' ? (
        <Grid container spacing={2}>
          {items.map((it) => {
            const st = stockState(it.currentStock);
            const purchased = Number(it.purchasedStock) || 0;
            const pct = purchased > 0 ? Math.min(100, Math.max(0, ((Number(it.currentStock) || 0) / purchased) * 100)) : 0;
            return (
              <Grid item xs={12} sm={6} md={3} xl={2} key={it.id ?? it.materialId}>
                <Card sx={{ height: '100%', minWidth: 0, borderColor: st.color === 'success' ? undefined : `${st.color}.main`, borderWidth: st.color === 'success' ? 1 : 2 }}>
                  <CardContent sx={{ minWidth: 0, p: { xs: 1.5, sm: 2 } }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 1, minWidth: 0 }}>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="subtitle1" fontWeight={700} sx={{ overflowWrap: 'anywhere' }}>
                          {it.materialName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {it.materialCode}
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        color={st.color}
                        label={st.label}
                        icon={st.color !== 'success' ? <WarningAmberIcon /> : undefined}
                        sx={{ maxWidth: '100%' }}
                      />
                    </Box>
                    <Typography variant="h4" sx={{ mt: 2 }} color={`${st.color}.main`}>
                      {formatNumber(it.currentStock)}{' '}
                      <Typography component="span" variant="body2" color="text.secondary">
                        {it.unit}
                      </Typography>
                    </Typography>
                    <LinearProgress variant="determinate" value={pct} color={st.color} sx={{ my: 1.5, height: 6, borderRadius: 3 }} />
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 0.5 }}>
                      <Typography variant="caption" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
                        Purchased: {formatNumber(it.purchasedStock)}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
                        Sold: {formatNumber(it.soldStock)}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      ) : (
        <Paper variant="outlined">
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Material</TableCell>
                  <TableCell>Unit</TableCell>
                  <TableCell align="right">Purchased</TableCell>
                  <TableCell align="right">Sold</TableCell>
                  <TableCell align="right">Current Stock</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {items.map((it) => {
                  const st = stockState(it.currentStock);
                  return (
                    <TableRow
                      key={it.id ?? it.materialId}
                      hover
                      sx={st.color !== 'success' ? (t) => ({ bgcolor: alpha(t.palette[st.color].main, 0.08) }) : undefined}
                    >
                      <TableCell>{it.materialCode}</TableCell>
                      <TableCell>{it.materialName}</TableCell>
                      <TableCell>{it.unit}</TableCell>
                      <TableCell align="right">{formatNumber(it.purchasedStock)}</TableCell>
                      <TableCell align="right">{formatNumber(it.soldStock)}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700 }}>
                        {formatNumber(it.currentStock)}
                      </TableCell>
                      <TableCell>
                        <Chip size="small" color={st.color} label={st.label} />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </>
  );
}

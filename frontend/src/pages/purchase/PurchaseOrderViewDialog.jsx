import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { purchaseOrderApi } from '../../api/purchaseOrderApi';
import { useNotify } from '../../context/NotificationContext';
import StatusChip from '../../components/StatusChip';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/errorMessage';

function Info({ label, children }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" component="div" fontWeight={500}>
        {children || '-'}
      </Typography>
    </Box>
  );
}

export default function PurchaseOrderViewDialog({ id, onClose }) {
  const notify = useNotify();
  const [po, setPo] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!id) {
      setPo(null);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    purchaseOrderApi
      .get(id)
      .then((d) => !cancelled && setPo(d))
      .catch((err) => {
        if (!cancelled) {
          notify.error(getErrorMessage(err, 'Failed to load purchase order'));
          onClose();
        }
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  return (
    <Dialog open={!!id} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
        Purchase Order {po?.poNumber}
        {po && <StatusChip status={po.status} />}
        {po && <StatusChip status={po.paymentStatus} />}
      </DialogTitle>
      <DialogContent dividers>
        {loading || !po ? (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}><Info label="Date">{formatDate(po.poDate)}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Supplier">{po.supplierName}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Vehicle">{po.vehicleNumber}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Created">{formatDateTime(po.createdAt)}</Info></Grid>
              <Grid item xs={12}><Info label="Remarks">{po.remarks}</Info></Grid>
            </Grid>

            <Box sx={{ overflowX: 'auto', mt: 3 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>#</TableCell>
                    <TableCell>Material</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Rate / Ton</TableCell>
                    <TableCell align="right">GST %</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell align="right">GST</TableCell>
                    <TableCell align="right">Total</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(po.items || []).map((it, i) => (
                    <TableRow key={it.id ?? i}>
                      <TableCell>{i + 1}</TableCell>
                      <TableCell>{it.materialName}</TableCell>
                      <TableCell align="right">{formatNumber(it.quantity)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.purchaseRatePerTon)}</TableCell>
                      <TableCell align="right">{formatNumber(it.gstPercentage)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.amount)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.gstAmount)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.totalAmount)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>

            <Divider sx={{ my: 2 }} />
            <Grid container spacing={2} justifyContent="flex-end">
              <Grid item xs={6} md={2}><Info label="Quantity">{formatNumber(po.totalQuantity)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Amount">{formatCurrency(po.amount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="GST">{formatCurrency(po.gstAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Total">{formatCurrency(po.totalAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Paid">{formatCurrency(po.paidAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Balance">{formatCurrency(po.balanceAmount)}</Info></Grid>
            </Grid>
          </>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

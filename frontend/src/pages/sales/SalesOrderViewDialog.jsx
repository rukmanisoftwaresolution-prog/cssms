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
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { salesOrderApi } from '../../api/salesOrderApi';
import { useNotify } from '../../context/NotificationContext';
import StatusChip from '../../components/StatusChip';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/errorMessage';
import { downloadInvoice } from './invoice';

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

export default function SalesOrderViewDialog({ id, onClose }) {
  const notify = useNotify();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!id) {
      setOrder(null);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    salesOrderApi
      .get(id)
      .then((d) => !cancelled && setOrder(d))
      .catch((err) => {
        if (!cancelled) {
          notify.error(getErrorMessage(err, 'Failed to load sales order'));
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
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
        Sales Order {order?.salesOrderNumber}
        {order && <StatusChip status={order.status} />}
        {order && <StatusChip status={order.paymentStatus} />}
      </DialogTitle>
      <DialogContent dividers>
        {loading || !order ? (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}><Info label="Invoice No.">{order.invoiceNumber}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Date">{formatDate(order.orderDate)}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Customer">{order.customerName}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Vehicle">{order.vehicleNumber}</Info></Grid>
              <Grid item xs={12} md={6}><Info label="Delivery Address">{order.deliveryAddress}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Remarks">{order.remarks}</Info></Grid>
              <Grid item xs={6} md={3}><Info label="Created">{formatDateTime(order.createdAt)}</Info></Grid>
            </Grid>

            <Box sx={{ overflowX: 'auto', mt: 3 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>#</TableCell>
                    <TableCell>Material</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Rate</TableCell>
                    <TableCell align="right">Discount</TableCell>
                    <TableCell align="right">GST %</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell align="right">GST</TableCell>
                    <TableCell align="right">Total</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(order.items || []).map((it, i) => (
                    <TableRow key={it.id ?? i}>
                      <TableCell>{i + 1}</TableCell>
                      <TableCell>{it.materialName}</TableCell>
                      <TableCell align="right">{formatNumber(it.quantity)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.rate)}</TableCell>
                      <TableCell align="right">{formatCurrency(it.discount)}</TableCell>
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
              <Grid item xs={6} md={2}><Info label="Sub Total">{formatCurrency(order.subTotal)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Discount">{formatCurrency(order.discountTotal)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="GST">{formatCurrency(order.gstAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Total">{formatCurrency(order.totalAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Advance">{formatCurrency(order.advanceAmount)}</Info></Grid>
              <Grid item xs={6} md={2}><Info label="Received">{formatCurrency(order.receivedAmount)}</Info></Grid>
              <Grid item xs={12} md={12} sx={{ textAlign: 'right' }}>
                <Typography variant="caption" color="text.secondary">
                  Pending Amount
                </Typography>
                <Typography variant="h6" color={Number(order.pendingAmount) > 0 ? 'error' : 'success.main'} fontWeight={700}>
                  {formatCurrency(order.pendingAmount)}
                </Typography>
              </Grid>
            </Grid>
          </>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        {order && (
          <Button startIcon={<PictureAsPdfIcon />} color="secondary" onClick={() => downloadInvoice(order, notify)}>
            Download Invoice
          </Button>
        )}
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useFieldArray, useForm, useWatch } from 'react-hook-form';
import {
  Alert,
  Box,
  Button,
  Divider,
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { salesOrderApi } from '../../api/salesOrderApi';
import { inventoryApi } from '../../api/inventoryApi';
import { useNotify } from '../../context/NotificationContext';
import { useCustomers, useMaterials } from '../../hooks/useLookups';
import PageHeader from '../../components/PageHeader';
import { FormDatePicker, FormSelect, FormTextField } from '../../components/FormFields';
import { PageLoader } from '../../components/LoadingOverlay';
import { DEFAULT_GST, PAYMENT_MODE_OPTIONS } from '../../utils/constants';
import { round2, formatCurrency, formatNumber, today, toNumber } from '../../utils/formatters';
import { salesLine, sumBy } from '../../utils/calc';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { combine, rules } from '../../utils/validators';

const blankItem = () => ({ materialId: '', quantity: '', rate: '', discount: 0, gstPercentage: DEFAULT_GST });

const gstRule = combine(rules.nonNegative('GST %'), {
  validate: (v) => Number(v) <= 100 || 'GST % cannot exceed 100',
});

export default function SalesOrderFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const notify = useNotify();
  const customers = useCustomers(true);
  const materials = useMaterials();
  const [loading, setLoading] = useState(isEdit);
  const [existing, setExisting] = useState(null);
  const [stock, setStock] = useState({});

  const {
    control,
    handleSubmit,
    setError,
    reset,
    getValues,
    formState: { isSubmitting, errors },
  } = useForm({
    defaultValues: {
      orderDate: today(),
      customerId: '',
      deliveryAddress: '',
      vehicleNumber: '',
      advanceAmount: 0,
      advancePaymentMode: '',
      remarks: '',
      items: [blankItem()],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'items' });
  const items = useWatch({ control, name: 'items' }) || [];
  const customerId = useWatch({ control, name: 'customerId' });
  const advance = toNumber(useWatch({ control, name: 'advanceAmount' }));

  useEffect(() => {
    inventoryApi
      .list()
      .then((list) => setStock(Object.fromEntries((list || []).map((s) => [s.materialId, s.currentStock]))))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!isEdit) return undefined;
    let cancelled = false;
    salesOrderApi
      .get(id)
      .then((so) => {
        if (cancelled) return;
        setExisting(so);
        reset({
          orderDate: so.orderDate,
          customerId: so.customerId,
          deliveryAddress: so.deliveryAddress || '',
          vehicleNumber: so.vehicleNumber || '',
          advanceAmount: so.advanceAmount ?? 0,
          advancePaymentMode: '',
          remarks: so.remarks || '',
          items: (so.items || []).map((it) => ({
            materialId: it.materialId,
            quantity: it.quantity,
            rate: it.rate,
            discount: it.discount ?? 0,
            gstPercentage: it.gstPercentage,
          })),
        });
      })
      .catch((err) => {
        notify.error(getErrorMessage(err, 'Failed to load sales order'));
        navigate('/sales-orders', { replace: true });
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id, isEdit, navigate, notify, reset]);

  const customerOptions = useMemo(() => {
    const opts = [...customers.options];
    if (existing && !opts.some((o) => o.value === existing.customerId)) {
      opts.push({ value: existing.customerId, label: existing.customerName });
    }
    return opts;
  }, [customers.options, existing]);

  const selectedCustomer = customers.items.find((c) => c.id === Number(customerId));

  const lines = items.map((it) => salesLine(it));
  const totals = {
    quantity: sumBy(items, 'quantity'),
    subTotal: sumBy(lines, 'gross'),
    discount: sumBy(lines, 'discount'),
    gst: sumBy(lines, 'gstAmount'),
    total: sumBy(lines, 'totalAmount'),
  };
  const pending = round2(totals.total - advance);

  const onSubmit = async (values) => {
    const adv = toNumber(values.advanceAmount);
    const payload = {
      orderDate: values.orderDate,
      customerId: Number(values.customerId),
      deliveryAddress: values.deliveryAddress?.trim() || null,
      vehicleNumber: values.vehicleNumber?.trim() || null,
      advanceAmount: adv,
      advancePaymentMode: adv > 0 && values.advancePaymentMode ? values.advancePaymentMode : null,
      remarks: values.remarks?.trim() || null,
      items: values.items.map((it) => ({
        materialId: Number(it.materialId),
        quantity: Number(it.quantity),
        rate: Number(it.rate),
        discount: toNumber(it.discount),
        gstPercentage: toNumber(it.gstPercentage),
      })),
    };
    try {
      if (isEdit) await salesOrderApi.update(id, payload);
      else await salesOrderApi.create(payload);
      notify.success(`Sales order ${isEdit ? 'updated' : 'created'} successfully`);
      navigate('/sales-orders');
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  if (loading) return <PageLoader />;

  return (
    <>
      <PageHeader title={isEdit ? `Edit Sales Order ${existing?.salesOrderNumber || ''}` : 'New Sales Order'}>
        <Button startIcon={<ArrowBackIcon />} color="inherit" onClick={() => navigate('/sales-orders')}>
          Back
        </Button>
      </PageHeader>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, mb: 3, minWidth: 0 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <FormDatePicker name="orderDate" control={control} label="Order Date" required rules={rules.required('Order date')} />
            </Grid>
            <Grid item xs={12} sm={6} md={5}>
              <FormSelect
                name="customerId"
                control={control}
                label="Customer"
                required
                options={customerOptions}
                loading={customers.loading}
                rules={rules.required('Customer')}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <FormTextField name="vehicleNumber" control={control} label="Vehicle Number" inputProps={{ style: { textTransform: 'uppercase' } }} />
            </Grid>
            {selectedCustomer && (
              <Grid item xs={12}>
                <Alert severity="info" icon={false} sx={{ py: 0.5 }}>
                  Credit limit: <b>{formatCurrency(selectedCustomer.creditLimit)}</b> &nbsp;|&nbsp; Outstanding:{' '}
                  <b>{formatCurrency(selectedCustomer.outstandingAmount)}</b> &nbsp;|&nbsp; Available credit:{' '}
                  <b>{formatCurrency(toNumber(selectedCustomer.creditLimit) - toNumber(selectedCustomer.outstandingAmount))}</b>
                </Alert>
              </Grid>
            )}
            <Grid item xs={12} sm={6}>
              <FormTextField name="deliveryAddress" control={control} label="Delivery Address" multiline minRows={2} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormTextField name="remarks" control={control} label="Remarks" multiline minRows={2} />
            </Grid>
          </Grid>
        </Paper>

        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3, lg: 1.5 }, mb: 3, minWidth: 0 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1, mb: 2 }}>
            <Typography variant="h6">Items</Typography>
            <Button startIcon={<AddIcon />} onClick={() => append(blankItem())} sx={{ width: { xs: '100%', sm: 'auto' } }}>
              Add Item
            </Button>
          </Box>
          {(errors.items?.root?.message || errors.items?.message) && (
            <Alert severity="error">{errors.items?.root?.message || errors.items?.message}</Alert>
          )}
          <Box sx={{ overflowX: 'auto', width: '100%', maxWidth: '100%', minWidth: 0, WebkitOverflowScrolling: 'touch' }}>
            <Table size="small" sx={{ minWidth: 1080 }}>
              <TableHead>
                <TableRow>
                  <TableCell width={40}>#</TableCell>
                  <TableCell sx={{ minWidth: 210 }}>Material</TableCell>
                  <TableCell width={115}>Quantity</TableCell>
                  <TableCell width={120}>Rate</TableCell>
                  <TableCell width={110}>Discount</TableCell>
                  <TableCell width={90}>GST %</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell align="right">GST</TableCell>
                  <TableCell align="right">Total</TableCell>
                  <TableCell width={50} />
                </TableRow>
              </TableHead>
              <TableBody>
                {fields.map((f, i) => {
                  const mid = items[i]?.materialId;
                  const available = mid !== '' && mid !== undefined ? stock[Number(mid)] : undefined;
                  return (
                    <TableRow key={f.id} sx={{ '& td': { verticalAlign: 'top', pt: 1.5 } }}>
                      <TableCell sx={{ pt: '22px !important' }}>{i + 1}</TableCell>
                      <TableCell>
                        <FormSelect
                          name={`items.${i}.materialId`}
                          control={control}
                          label="Material"
                          options={materials.options}
                          loading={materials.loading}
                          rules={rules.required('Material')}
                          helperText={available !== undefined ? `Available: ${formatNumber(available)}` : undefined}
                        />
                      </TableCell>
                      <TableCell>
                        <FormTextField
                          name={`items.${i}.quantity`}
                          control={control}
                          type="number"
                          label="Qty"
                          rules={combine(rules.positive('Quantity'), {
                            validate: () => {
                              const m = getValues(`items.${i}.materialId`);
                              const avail = m ? stock[Number(m)] : undefined;
                              if (isEdit || avail === undefined) return true;
                              const totalForMaterial = getValues('items')
                                .filter((x) => String(x.materialId) === String(m))
                                .reduce((s, x) => s + toNumber(x.quantity), 0);
                              return totalForMaterial <= avail || `Only ${formatNumber(avail)} in stock`;
                            },
                          })}
                        />
                      </TableCell>
                      <TableCell>
                        <FormTextField name={`items.${i}.rate`} control={control} type="number" label="Rate" rules={rules.positive('Rate')} />
                      </TableCell>
                      <TableCell>
                        <FormTextField
                          name={`items.${i}.discount`}
                          control={control}
                          type="number"
                          label="Discount"
                          rules={combine(rules.nonNegative('Discount', false), {
                            validate: (v) => {
                              const it = getValues(`items.${i}`);
                              return toNumber(v) <= toNumber(it.quantity) * toNumber(it.rate) || 'Exceeds line amount';
                            },
                          })}
                        />
                      </TableCell>
                      <TableCell>
                        <FormTextField name={`items.${i}.gstPercentage`} control={control} type="number" label="GST %" rules={gstRule} />
                      </TableCell>
                      <TableCell align="right" sx={{ pt: '22px !important' }}>{formatCurrency(lines[i]?.amount)}</TableCell>
                      <TableCell align="right" sx={{ pt: '22px !important' }}>{formatCurrency(lines[i]?.gstAmount)}</TableCell>
                      <TableCell align="right" sx={{ pt: '22px !important', fontWeight: 600 }}>
                        {formatCurrency(lines[i]?.totalAmount)}
                      </TableCell>
                      <TableCell sx={{ pt: '14px !important' }}>
                        <Tooltip title="Remove item">
                          <span>
                            <IconButton aria-label="Remove item" color="error" disabled={fields.length === 1} onClick={() => remove(i)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        </Paper>

        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, mb: 3, minWidth: 0 }}>
          <Grid container spacing={3} alignItems="flex-start">
            <Grid item xs={12} md={6}>
              <Typography variant="h6" gutterBottom>
                Advance Payment
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <FormTextField
                    name="advanceAmount"
                    control={control}
                    type="number"
                    label="Advance Amount"
                    rules={combine(rules.nonNegative('Advance', false), {
                      validate: (v) => toNumber(v) <= totals.total || 'Advance cannot exceed order total',
                    })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <FormSelect
                    name="advancePaymentMode"
                    control={control}
                    label="Payment Mode"
                    emptyLabel="Select mode"
                    options={PAYMENT_MODE_OPTIONS}
                    rules={{
                      validate: (v) => !(advance > 0 && !isEdit && !v) || 'Payment mode is required with an advance',
                    }}
                  />
                </Grid>
              </Grid>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'minmax(0, 1fr)', sm: 'repeat(2, minmax(0, 1fr))' }, gap: 2, textAlign: 'right' }}>
                <Total label="Total Quantity" value={formatNumber(totals.quantity)} />
                <Total label="Sub Total" value={formatCurrency(totals.subTotal)} />
                <Total label="Discount" value={formatCurrency(totals.discount)} />
                <Total label="GST" value={formatCurrency(totals.gst)} />
              </Box>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 4, flexWrap: 'wrap' }}>
                <Total label="Grand Total" value={formatCurrency(totals.total)} big />
                <Total label="Advance" value={formatCurrency(advance)} />
                <Total label="Pending Amount" value={formatCurrency(pending)} big color={pending > 0 ? 'error' : 'success.main'} />
              </Box>
            </Grid>
          </Grid>
        </Paper>

        <Box sx={{ display: 'flex', flexDirection: { xs: 'column-reverse', sm: 'row' }, justifyContent: 'flex-end', gap: 1, width: '100%' }}>
          <Button color="inherit" onClick={() => navigate('/sales-orders')} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} sx={{ width: { xs: '100%', sm: 'auto' } }}>
            {isSubmitting ? 'Saving...' : isEdit ? 'Update Sales Order' : 'Create Sales Order'}
          </Button>
        </Box>
      </form>
    </>
  );
}

function Total({ label, value, big, color }) {
  return (
    <Box sx={{ textAlign: 'right' }}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant={big ? 'h6' : 'subtitle1'} fontWeight={700} color={color || (big ? 'primary' : undefined)}>
        {value}
      </Typography>
    </Box>
  );
}

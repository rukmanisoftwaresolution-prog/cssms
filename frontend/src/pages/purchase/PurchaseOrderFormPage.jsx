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
import { purchaseOrderApi } from '../../api/purchaseOrderApi';
import { useNotify } from '../../context/NotificationContext';
import { useMaterials, useSuppliers } from '../../hooks/useLookups';
import PageHeader from '../../components/PageHeader';
import { FormDatePicker, FormSelect, FormTextField } from '../../components/FormFields';
import { PageLoader } from '../../components/LoadingOverlay';
import { DEFAULT_GST } from '../../utils/constants';
import { purchaseLine, sumBy } from '../../utils/calc';
import { formatCurrency, formatNumber, today } from '../../utils/formatters';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { combine, rules } from '../../utils/validators';

const blankItem = () => ({ materialId: '', quantity: '', purchaseRatePerTon: '', gstPercentage: DEFAULT_GST });

const gstRule = combine(rules.nonNegative('GST %'), {
  validate: (v) => Number(v) <= 100 || 'GST % cannot exceed 100',
});

export default function PurchaseOrderFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const notify = useNotify();
  const suppliers = useSuppliers(true);
  const materials = useMaterials();
  const [loading, setLoading] = useState(isEdit);
  const [existing, setExisting] = useState(null);

  const {
    control,
    handleSubmit,
    setError,
    reset,
    formState: { isSubmitting, errors },
  } = useForm({
    defaultValues: { poDate: today(), supplierId: '', vehicleNumber: '', remarks: '', items: [blankItem()] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'items' });
  const items = useWatch({ control, name: 'items' }) || [];

  useEffect(() => {
    if (!isEdit) return undefined;
    let cancelled = false;
    purchaseOrderApi
      .get(id)
      .then((po) => {
        if (cancelled) return;
        setExisting(po);
        reset({
          poDate: po.poDate,
          supplierId: po.supplierId,
          vehicleNumber: po.vehicleNumber || '',
          remarks: po.remarks || '',
          items: (po.items || []).map((it) => ({
            materialId: it.materialId,
            quantity: it.quantity,
            purchaseRatePerTon: it.purchaseRatePerTon,
            gstPercentage: it.gstPercentage,
          })),
        });
      })
      .catch((err) => {
        notify.error(getErrorMessage(err, 'Failed to load purchase order'));
        navigate('/purchase-orders', { replace: true });
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id, isEdit, navigate, notify, reset]);

  const supplierOptions = useMemo(() => {
    const opts = [...suppliers.options];
    if (existing && !opts.some((o) => o.value === existing.supplierId)) {
      opts.push({ value: existing.supplierId, label: existing.supplierName });
    }
    return opts;
  }, [suppliers.options, existing]);

  const lines = items.map((it) => purchaseLine(it));
  const totals = {
    quantity: sumBy(items, 'quantity'),
    amount: sumBy(lines, 'amount'),
    gst: sumBy(lines, 'gstAmount'),
    total: sumBy(lines, 'totalAmount'),
  };

  const onSubmit = async (values) => {
    const payload = {
      poDate: values.poDate,
      supplierId: Number(values.supplierId),
      vehicleNumber: values.vehicleNumber?.trim() || null,
      remarks: values.remarks?.trim() || null,
      items: values.items.map((it) => ({
        materialId: Number(it.materialId),
        quantity: Number(it.quantity),
        purchaseRatePerTon: Number(it.purchaseRatePerTon),
        gstPercentage: Number(it.gstPercentage || 0),
      })),
    };
    try {
      if (isEdit) await purchaseOrderApi.update(id, payload);
      else await purchaseOrderApi.create(payload);
      notify.success(`Purchase order ${isEdit ? 'updated' : 'created'} successfully`);
      navigate('/purchase-orders');
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  if (loading) return <PageLoader />;

  return (
    <>
      <PageHeader title={isEdit ? `Edit Purchase Order ${existing?.poNumber || ''}` : 'New Purchase Order'}>
        <Button startIcon={<ArrowBackIcon />} color="inherit" onClick={() => navigate('/purchase-orders')}>
          Back
        </Button>
      </PageHeader>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, mb: 3, minWidth: 0 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <FormDatePicker name="poDate" control={control} label="PO Date" required rules={rules.required('PO date')} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <FormSelect
                name="supplierId"
                control={control}
                label="Supplier"
                required
                options={supplierOptions}
                loading={suppliers.loading}
                rules={rules.required('Supplier')}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2.5}>
              <FormTextField name="vehicleNumber" control={control} label="Vehicle Number" inputProps={{ style: { textTransform: 'uppercase' } }} />
            </Grid>
            <Grid item xs={12} sm={6} md={2.5}>
              <FormTextField name="remarks" control={control} label="Remarks" />
            </Grid>
          </Grid>
        </Paper>

        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, mb: 3, minWidth: 0 }}>
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
            <Table size="small" sx={{ minWidth: 980 }}>
              <TableHead>
                <TableRow>
                  <TableCell width={40}>#</TableCell>
                  <TableCell sx={{ minWidth: 200 }}>Material</TableCell>
                  <TableCell width={120}>Quantity (Ton)</TableCell>
                  <TableCell width={130}>Rate / Ton</TableCell>
                  <TableCell width={100}>GST %</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell align="right">GST</TableCell>
                  <TableCell align="right">Total</TableCell>
                  <TableCell width={50} />
                </TableRow>
              </TableHead>
              <TableBody>
                {fields.map((f, i) => (
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
                      />
                    </TableCell>
                    <TableCell>
                      <FormTextField
                        name={`items.${i}.quantity`}
                        control={control}
                        type="number"
                        label="Qty"
                        rules={rules.positive('Quantity')}
                      />
                    </TableCell>
                    <TableCell>
                      <FormTextField
                        name={`items.${i}.purchaseRatePerTon`}
                        control={control}
                        type="number"
                        label="Rate"
                        rules={rules.positive('Rate')}
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
                ))}
              </TableBody>
            </Table>
          </Box>

          <Divider sx={{ my: 2 }} />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 4, flexWrap: 'wrap' }}>
            <Total label="Total Quantity" value={`${formatNumber(totals.quantity)} Ton`} />
            <Total label="Amount" value={formatCurrency(totals.amount)} />
            <Total label="GST" value={formatCurrency(totals.gst)} />
            <Total label="Grand Total" value={formatCurrency(totals.total)} big />
          </Box>
        </Paper>

        <Box sx={{ display: 'flex', flexDirection: { xs: 'column-reverse', sm: 'row' }, justifyContent: 'flex-end', gap: 1, width: '100%' }}>
          <Button color="inherit" onClick={() => navigate('/purchase-orders')} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} sx={{ width: { xs: '100%', sm: 'auto' } }}>
            {isSubmitting ? 'Saving...' : isEdit ? 'Update Purchase Order' : 'Create Purchase Order'}
          </Button>
        </Box>
      </form>
    </>
  );
}

function Total({ label, value, big }) {
  return (
    <Box sx={{ textAlign: 'right' }}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant={big ? 'h6' : 'subtitle1'} fontWeight={700} color={big ? 'primary' : undefined}>
        {value}
      </Typography>
    </Box>
  );
}

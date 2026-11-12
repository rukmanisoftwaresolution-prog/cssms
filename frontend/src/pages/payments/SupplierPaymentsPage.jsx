import { supplierPaymentApi } from '../../api/supplierPaymentApi';
import { purchaseOrderApi } from '../../api/purchaseOrderApi';
import { useSuppliers } from '../../hooks/useLookups';
import { formatCurrency } from '../../utils/formatters';
import PaymentsPage from './PaymentsPage';

const config = {
  title: 'Supplier Payments',
  subtitle: 'Payments made against purchase orders',
  singular: 'Supplier payment',
  dialogTitle: 'Make Supplier Payment',
  addLabel: 'Make Payment',
  submitLabel: 'Pay',
  permission: 'supplierPayments',
  api: supplierPaymentApi,
  useParties: () => useSuppliers(true),
  partyField: 'supplierId',
  partyLabel: 'Supplier',
  partyNameField: 'supplierName',
  docField: 'purchaseOrderId',
  docLabel: 'Purchase Order',
  docPlural: 'purchase orders',
  docNumberField: 'poNumber',
  amountField: 'amountPaid',
  loadPending: (supplierId) => purchaseOrderApi.list({ supplierId, pendingOnly: true, size: 1000 }),
  pendingOf: (po) => Number(po.balanceAmount) || 0,
  docOptionLabel: (po) => `${po.poNumber} - Balance ${formatCurrency(po.balanceAmount)} (Total ${formatCurrency(po.totalAmount)})`,
};

export default function SupplierPaymentsPage() {
  return <PaymentsPage config={config} />;
}

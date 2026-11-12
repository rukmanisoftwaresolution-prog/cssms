import { customerPaymentApi } from '../../api/customerPaymentApi';
import { salesOrderApi } from '../../api/salesOrderApi';
import { useCustomers } from '../../hooks/useLookups';
import { formatCurrency } from '../../utils/formatters';
import PaymentsPage from './PaymentsPage';

const config = {
  title: 'Customer Payments',
  subtitle: 'Receipts against sales invoices',
  singular: 'Customer payment',
  dialogTitle: 'Receive Payment',
  addLabel: 'Receive Payment',
  submitLabel: 'Receive',
  permission: 'customerPayments',
  api: customerPaymentApi,
  useParties: () => useCustomers(true),
  partyField: 'customerId',
  partyLabel: 'Customer',
  partyNameField: 'customerName',
  docField: 'salesOrderId',
  docLabel: 'Invoice',
  docPlural: 'invoices',
  docNumberField: 'invoiceNumber',
  amountField: 'amountReceived',
  loadPending: (customerId) => salesOrderApi.list({ customerId, pendingOnly: true, size: 1000 }),
  pendingOf: (so) => Number(so.pendingAmount) || 0,
  docOptionLabel: (so) => `${so.invoiceNumber} - Pending ${formatCurrency(so.pendingAmount)} (Total ${formatCurrency(so.totalAmount)})`,
};

export default function CustomerPaymentsPage() {
  return <PaymentsPage config={config} />;
}

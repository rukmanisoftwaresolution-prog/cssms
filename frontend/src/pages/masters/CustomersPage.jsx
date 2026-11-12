import { customerApi } from '../../api/customerApi';
import StatusChip from '../../components/StatusChip';
import CurrencyText from '../../components/CurrencyText';
import { STATUS_OPTIONS } from '../../utils/constants';
import { combine, rules } from '../../utils/validators';
import MasterCrudPage from './MasterCrudPage';

const config = {
  title: 'Customers',
  singular: 'Customer',
  subtitle: 'Manage customers and credit limits',
  permission: 'customers',
  api: customerApi,
  hasStatus: true,
  searchPlaceholder: 'Search by name, company, code, mobile...',
  rowLabel: (r) => r.customerName,
  columns: [
    { key: 'customerCode', label: 'Code', sortable: true },
    { key: 'customerName', label: 'Customer', sortable: true },
    { key: 'companyName', label: 'Company', sortable: true },
    { key: 'mobileNumber', label: 'Mobile' },
    { key: 'gstNumber', label: 'GSTIN' },
    { key: 'creditLimit', label: 'Credit Limit', align: 'right', sortable: true, render: (r) => <CurrencyText value={r.creditLimit} /> },
    { key: 'outstandingAmount', label: 'Outstanding', align: 'right', render: (r) => <CurrencyText value={r.outstandingAmount} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusChip status={r.status} /> },
  ],
  fields: [
    { name: 'customerName', label: 'Customer Name', required: true, rules: rules.required('Customer name') },
    { name: 'companyName', label: 'Company Name' },
    { name: 'mobileNumber', label: 'Mobile Number', required: true, rules: rules.mobile(true) },
    { name: 'email', label: 'Email', rules: rules.email(false) },
    { name: 'gstNumber', label: 'GSTIN', upper: true, rules: rules.gst(), helperText: 'Optional - 15 characters' },
    { name: 'creditLimit', label: 'Credit Limit', type: 'number', default: 0, required: true, rules: combine(rules.required('Credit limit'), rules.nonNegative('Credit limit')) },
    { name: 'status', label: 'Status', type: 'select', options: STATUS_OPTIONS, default: 'ACTIVE', required: true, rules: rules.required('Status') },
    { name: 'address', label: 'Address', type: 'textarea', required: true, rules: rules.required('Address') },
  ],
};

export default function CustomersPage() {
  return <MasterCrudPage config={config} />;
}

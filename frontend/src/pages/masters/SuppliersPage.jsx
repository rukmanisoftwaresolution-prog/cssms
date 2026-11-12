import { supplierApi } from '../../api/supplierApi';
import StatusChip from '../../components/StatusChip';
import CurrencyText from '../../components/CurrencyText';
import { STATUS_OPTIONS } from '../../utils/constants';
import { rules } from '../../utils/validators';
import MasterCrudPage from './MasterCrudPage';

const config = {
  title: 'Suppliers',
  singular: 'Supplier',
  subtitle: 'Manage stone suppliers',
  permission: 'suppliers',
  api: supplierApi,
  hasStatus: true,
  searchPlaceholder: 'Search by name, code, GST, mobile...',
  rowLabel: (r) => r.supplierName,
  columns: [
    { key: 'supplierCode', label: 'Code', sortable: true },
    { key: 'supplierName', label: 'Supplier', sortable: true },
    { key: 'contactPerson', label: 'Contact Person' },
    { key: 'mobileNumber', label: 'Mobile' },
    { key: 'gstNumber', label: 'GSTIN' },
    { key: 'outstandingAmount', label: 'Outstanding', align: 'right', render: (r) => <CurrencyText value={r.outstandingAmount} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusChip status={r.status} /> },
  ],
  fields: [
    { name: 'supplierName', label: 'Supplier Name', required: true, rules: rules.required('Supplier name') },
    { name: 'contactPerson', label: 'Contact Person', required: true, rules: rules.required('Contact person') },
    { name: 'mobileNumber', label: 'Mobile Number', required: true, rules: rules.mobile(true) },
    { name: 'gstNumber', label: 'GSTIN', upper: true, rules: rules.gst(), helperText: 'Optional - 15 characters' },
    { name: 'panNumber', label: 'PAN', upper: true, rules: rules.pan(), helperText: 'Optional - 10 characters' },
    { name: 'status', label: 'Status', type: 'select', options: STATUS_OPTIONS, default: 'ACTIVE', required: true, rules: rules.required('Status') },
    { name: 'address', label: 'Address', type: 'textarea', required: true, rules: rules.required('Address') },
    { name: 'bankDetails', label: 'Bank Details', type: 'textarea' },
  ],
};

export default function SuppliersPage() {
  return <MasterCrudPage config={config} />;
}

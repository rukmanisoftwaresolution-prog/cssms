import { employeeApi } from '../../api/employeeApi';
import StatusChip from '../../components/StatusChip';
import CurrencyText from '../../components/CurrencyText';
import { STATUS_OPTIONS } from '../../utils/constants';
import { formatDate } from '../../utils/formatters';
import { combine, rules } from '../../utils/validators';
import MasterCrudPage from './MasterCrudPage';

const config = {
  title: 'Employees',
  singular: 'Employee',
  subtitle: 'Manage employee records',
  permission: 'employees',
  api: employeeApi,
  hasStatus: true,
  searchPlaceholder: 'Search by name, code, mobile...',
  rowLabel: (r) => r.fullName,
  columns: [
    { key: 'employeeCode', label: 'Code', sortable: true },
    { key: 'fullName', label: 'Name', sortable: true },
    { key: 'designation', label: 'Designation', sortable: true },
    { key: 'mobileNumber', label: 'Mobile' },
    { key: 'email', label: 'Email' },
    { key: 'joiningDate', label: 'Joining Date', sortable: true, render: (r) => formatDate(r.joiningDate) },
    { key: 'salary', label: 'Salary', align: 'right', sortable: true, render: (r) => <CurrencyText value={r.salary} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusChip status={r.status} /> },
  ],
  fields: [
    { name: 'fullName', label: 'Full Name', required: true, rules: rules.required('Full name') },
    { name: 'designation', label: 'Designation', required: true, rules: rules.required('Designation') },
    { name: 'mobileNumber', label: 'Mobile Number', required: true, rules: rules.mobile(true) },
    { name: 'email', label: 'Email', rules: rules.email(false) },
    { name: 'joiningDate', label: 'Joining Date', type: 'date', required: true, rules: rules.required('Joining date') },
    { name: 'salary', label: 'Salary', type: 'number', required: true, rules: combine(rules.required('Salary'), rules.nonNegative('Salary')) },
    { name: 'address', label: 'Address', type: 'textarea' },
    { name: 'status', label: 'Status', type: 'select', options: STATUS_OPTIONS, default: 'ACTIVE', required: true, rules: rules.required('Status') },
  ],
};

export default function EmployeesPage() {
  return <MasterCrudPage config={config} />;
}

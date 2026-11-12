import { userApi } from '../../api/userApi';
import StatusChip from '../../components/StatusChip';
import { ROLE_OPTIONS } from '../../utils/constants';
import { combine, rules } from '../../utils/validators';
import MasterCrudPage from '../masters/MasterCrudPage';

const config = {
  title: 'Users',
  singular: 'User',
  subtitle: 'Manage system users and roles',
  permission: 'users',
  api: userApi,
  hasStatus: false,
  extraFilter: { name: 'role', label: 'Role', options: ROLE_OPTIONS },
  searchPlaceholder: 'Search by username, name, email...',
  rowLabel: (r) => r.username,
  columns: [
    { key: 'username', label: 'Username', sortable: true },
    { key: 'fullName', label: 'Full Name', sortable: true },
    { key: 'email', label: 'Email' },
    { key: 'mobileNumber', label: 'Mobile' },
    { key: 'role', label: 'Role', render: (r) => <StatusChip status={r.role} /> },
    { key: 'active', label: 'Status', render: (r) => <StatusChip status={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
  ],
  fields: [
    {
      name: 'username',
      label: 'Username',
      required: true,
      disabledOnEdit: true,
      rules: combine(rules.required('Username'), rules.maxLength(50, 'Username')),
    },
    { name: 'fullName', label: 'Full Name', required: true, rules: rules.required('Full name') },
    { name: 'email', label: 'Email', required: true, rules: rules.email(true) },
    { name: 'mobileNumber', label: 'Mobile Number', required: true, rules: rules.mobile(true) },
    { name: 'role', label: 'Role', type: 'select', options: ROLE_OPTIONS, default: 'SALES_EXECUTIVE', required: true, rules: rules.required('Role') },
    {
      name: 'password',
      label: 'Password',
      type: 'password',
      omitIfBlank: true,
      omitOnEdit: true,
      required: (isEdit) => !isEdit,
      rules: (isEdit) => rules.password(!isEdit),
      helperText: 'Min 8 chars, upper + lower + digit (leave blank to keep current)',
    },
    { name: 'active', label: 'Active', type: 'switch', default: true },
  ],
};

export default function UsersPage() {
  return <MasterCrudPage config={config} />;
}

import { materialApi } from '../../api/materialApi';
import { DEFAULT_UNIT } from '../../utils/constants';
import { rules } from '../../utils/validators';
import MasterCrudPage from './MasterCrudPage';

const config = {
  title: 'Materials',
  singular: 'Material',
  subtitle: 'Crushed stone products and units',
  permission: 'materials',
  api: materialApi,
  hasStatus: false,
  searchPlaceholder: 'Search by name or code...',
  rowLabel: (r) => r.materialName,
  columns: [
    { key: 'materialCode', label: 'Code', sortable: true },
    { key: 'materialName', label: 'Material', sortable: true },
    { key: 'unit', label: 'Unit' },
    { key: 'description', label: 'Description' },
  ],
  fields: [
    { name: 'materialName', label: 'Material Name', required: true, rules: rules.required('Material name') },
    { name: 'unit', label: 'Unit', default: DEFAULT_UNIT, upper: true, required: true, rules: rules.required('Unit'), helperText: 'e.g. TON' },
    { name: 'description', label: 'Description', type: 'textarea' },
  ],
};

export default function MaterialsPage() {
  return <MasterCrudPage config={config} />;
}

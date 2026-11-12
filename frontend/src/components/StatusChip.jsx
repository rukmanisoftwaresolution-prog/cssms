import { Chip } from '@mui/material';
import { humanize } from '../utils/formatters';

const COLORS = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  PAID: 'success',
  PARTIAL_PAID: 'warning',
  PENDING: 'error',
  CANCELLED: 'default',
  ADMIN: 'primary',
  MANAGER: 'info',
  SALES_EXECUTIVE: 'secondary',
};

export default function StatusChip({ status, label, size = 'small', variant = 'filled' }) {
  if (!status) return '-';
  const color = COLORS[status] || 'default';
  return (
    <Chip
      size={size}
      label={label || humanize(status)}
      color={color}
      variant={status === 'CANCELLED' || status === 'INACTIVE' ? 'outlined' : variant}
    />
  );
}

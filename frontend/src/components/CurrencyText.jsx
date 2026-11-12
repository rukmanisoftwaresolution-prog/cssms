import { Typography } from '@mui/material';
import { formatCurrency } from '../utils/formatters';

export default function CurrencyText({ value, color, ...rest }) {
  return (
    <Typography component="span" variant="inherit" color={color} sx={{ whiteSpace: 'nowrap' }} {...rest}>
      {formatCurrency(value)}
    </Typography>
  );
}

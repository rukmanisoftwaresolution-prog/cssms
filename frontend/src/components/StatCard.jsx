import { Box, Card, CardContent, Skeleton, Typography } from '@mui/material';
import { formatCurrency, formatNumber } from '../utils/formatters';

export default function StatCard({ title, value, icon, color = 'primary', format = 'currency', loading, caption }) {
  const display = format === 'currency' ? formatCurrency(value ?? 0) : format === 'number' ? formatNumber(value ?? 0) : value;
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            width: 52,
            height: 52,
            borderRadius: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: `${color}.main`,
            bgcolor: (t) => `${t.palette[color].main}1a`,
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" color="text.secondary" noWrap>
            {title}
          </Typography>
          {loading ? (
            <Skeleton width={110} height={36} />
          ) : (
            <Typography variant="h6" noWrap sx={{ fontWeight: 700 }}>
              {display}
            </Typography>
          )}
          {caption && (
            <Typography variant="caption" color="text.secondary">
              {caption}
            </Typography>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}

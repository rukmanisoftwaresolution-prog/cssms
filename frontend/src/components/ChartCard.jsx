import { Box, Card, CardContent, CardHeader, Skeleton, Typography } from '@mui/material';

export default function ChartCard({ title, children, height = 300, loading = false, empty = false }) {
  return (
    <Card sx={{ height: '100%', minWidth: 0, maxWidth: '100%' }}>
      <CardHeader title={title} titleTypographyProps={{ variant: 'subtitle1', fontWeight: 600 }} sx={{ pb: 0 }} />
      <CardContent>
        <Box sx={{ width: '100%', height, minWidth: 0, maxWidth: '100%' }}>
          {loading ? (
            <Skeleton variant="rounded" width="100%" height="100%" />
          ) : empty ? (
            <Box sx={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary" variant="body2">
                No data available
              </Typography>
            </Box>
          ) : (
            children
          )}
        </Box>
      </CardContent>
    </Card>
  );
}

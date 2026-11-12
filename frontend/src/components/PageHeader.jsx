import { Box, Stack, Typography } from '@mui/material';

export default function PageHeader({ title, subtitle, children }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' },
        alignItems: { xs: 'flex-start', sm: 'center' },
        justifyContent: 'space-between',
        gap: 2,
        mb: 3,
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="h5" component="h1">
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </Box>
      {children && (
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          flexWrap="wrap"
          useFlexGap
          sx={{
            width: { xs: '100%', sm: 'auto' },
            '& .MuiButton-root': { width: { xs: '100%', sm: 'auto' }, minHeight: 44 },
          }}
        >
          {children}
        </Stack>
      )}
    </Box>
  );
}

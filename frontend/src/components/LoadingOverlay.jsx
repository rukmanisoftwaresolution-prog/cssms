import { Backdrop, Box, CircularProgress } from '@mui/material';

/** `fullscreen` covers the viewport; otherwise covers the nearest positioned parent. */
export default function LoadingOverlay({ open = true, fullscreen = false }) {
  if (fullscreen) {
    return (
      <Backdrop open={open} sx={{ zIndex: (t) => t.zIndex.modal + 1, color: '#fff' }}>
        <CircularProgress color="inherit" />
      </Backdrop>
    );
  }
  if (!open) return null;
  return (
    <Box
      sx={{
        position: 'absolute',
        inset: 0,
        zIndex: 5,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'rgba(255,255,255,0.6)',
      }}
    >
      <CircularProgress />
    </Box>
  );
}

export function PageLoader({ minHeight = '60vh' }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight }}>
      <CircularProgress />
    </Box>
  );
}

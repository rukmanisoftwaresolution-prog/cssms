import { Box, Button, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import BlockIcon from '@mui/icons-material/Block';

export default function ForbiddenPage() {
  const navigate = useNavigate();
  return (
    <Box sx={{ textAlign: 'center', py: 10 }}>
      <BlockIcon color="error" sx={{ fontSize: 72 }} />
      <Typography variant="h3" fontWeight={700} sx={{ mt: 1 }}>
        403
      </Typography>
      <Typography variant="h6" gutterBottom>
        Access denied
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        You do not have permission to view this page.
      </Typography>
      <Button variant="contained" onClick={() => navigate('/')}>
        Back to Dashboard
      </Button>
    </Box>
  );
}

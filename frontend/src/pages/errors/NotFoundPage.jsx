import { Box, Button, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import SearchOffIcon from '@mui/icons-material/SearchOff';

export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Box sx={{ textAlign: 'center', py: 10 }}>
      <SearchOffIcon color="disabled" sx={{ fontSize: 72 }} />
      <Typography variant="h3" fontWeight={700} sx={{ mt: 1 }}>
        404
      </Typography>
      <Typography variant="h6" gutterBottom>
        Page not found
      </Typography>
      <Button variant="contained" onClick={() => navigate('/')} sx={{ mt: 2 }}>
        Back to Dashboard
      </Button>
    </Box>
  );
}

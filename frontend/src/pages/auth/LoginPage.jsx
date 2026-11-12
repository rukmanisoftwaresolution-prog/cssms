import { useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  InputAdornment,
  Link,
  Paper,
  Typography,
} from '@mui/material';
import TerrainIcon from '@mui/icons-material/Terrain';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import { authApi } from '../../api/authApi';
import { FormTextField } from '../../components/FormFields';
import { getErrorMessage, handleFormError } from '../../utils/errorMessage';
import { rules } from '../../utils/validators';

function ForgotPasswordDialog({ open, onClose }) {
  const notify = useNotify();
  const [result, setResult] = useState(null);
  const {
    control,
    handleSubmit,
    setError,
    reset,
    formState: { isSubmitting },
  } = useForm({ defaultValues: { email: '' } });

  const close = () => {
    setResult(null);
    reset();
    onClose();
  };

  const onSubmit = async ({ email }) => {
    try {
      const data = await authApi.forgotPassword(email.trim());
      setResult(data || { message: 'If the email exists, a reset link has been sent.' });
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  return (
    <Dialog open={open} onClose={close} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogTitle>Forgot password</DialogTitle>
        <DialogContent>
          {result ? (
            <Box>
              <Alert severity="success" sx={{ mb: 2 }}>
                {result.message}
              </Alert>
              {result.resetToken && (
                <Alert severity="info">
                  Dev mode reset token: <code style={{ wordBreak: 'break-all' }}>{result.resetToken}</code>
                  <Box sx={{ mt: 1 }}>
                    <Link component={RouterLink} to={`/reset-password?token=${encodeURIComponent(result.resetToken)}`}>
                      Open reset password page
                    </Link>
                  </Box>
                </Alert>
              )}
            </Box>
          ) : (
            <>
              <DialogContentText sx={{ mb: 2 }}>
                Enter your registered email address. We will send you a link to reset your password.
              </DialogContentText>
              <FormTextField
                name="email"
                control={control}
                label="Email"
                autoFocus
                rules={rules.email(true)}
              />
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={close} color="inherit">
            {result ? 'Close' : 'Cancel'}
          </Button>
          {!result && (
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? 'Sending...' : 'Send reset link'}
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [forgotOpen, setForgotOpen] = useState(false);
  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm({ defaultValues: { username: '', password: '' } });

  const onSubmit = async ({ username, password }) => {
    setError('');
    try {
      await login(username.trim(), password);
      const from = location.state?.from;
      navigate(from?.pathname ? `${from.pathname}${from.search || ''}` : '/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Login failed'));
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
        background: 'linear-gradient(135deg, #0f2f45 0%, #1b4965 55%, #4c7a96 100%)',
      }}
    >
      <Paper sx={{ p: { xs: 3, sm: 5 }, width: '100%', maxWidth: 430 }} elevation={8}>
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Avatar sx={{ bgcolor: 'secondary.main', width: 56, height: 56, mx: 'auto', mb: 1.5 }}>
            <TerrainIcon fontSize="large" />
          </Avatar>
          <Typography variant="h5">CSSMS</Typography>
          <Typography variant="body2" color="text.secondary">
            Crusher Stone Supply Management System
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <FormTextField
            name="username"
            control={control}
            label="Username"
            autoFocus
            autoComplete="username"
            rules={rules.required('Username')}
          />
          <FormTextField
            name="password"
            control={control}
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            rules={rules.required('Password')}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowPassword((s) => !s)} edge="end" aria-label="toggle password">
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <Button type="submit" variant="contained" size="large" fullWidth disabled={isSubmitting} sx={{ mt: 1 }}>
            {isSubmitting ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Link component="button" type="button" variant="body2" onClick={() => setForgotOpen(true)}>
            Forgot password?
          </Link>
        </Box>
      </Paper>
      <ForgotPasswordDialog open={forgotOpen} onClose={() => setForgotOpen(false)} />
    </Box>
  );
}

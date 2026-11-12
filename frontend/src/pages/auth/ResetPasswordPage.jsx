import { useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Alert, Avatar, Box, Button, Link, Paper, Typography } from '@mui/material';
import LockResetIcon from '@mui/icons-material/LockReset';
import { authApi } from '../../api/authApi';
import { useNotify } from '../../context/NotificationContext';
import { FormTextField } from '../../components/FormFields';
import { handleFormError } from '../../utils/errorMessage';
import { PASSWORD_HINT, rules } from '../../utils/validators';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const notify = useNotify();
  const token = params.get('token') || '';
  const [done, setDone] = useState(false);
  const {
    control,
    handleSubmit,
    setError,
    getValues,
    formState: { isSubmitting },
  } = useForm({ defaultValues: { newPassword: '', confirmPassword: '' } });

  const onSubmit = async ({ newPassword }) => {
    try {
      await authApi.resetPassword(token, newPassword);
      setDone(true);
      notify.success('Password reset successfully. Please sign in.');
      setTimeout(() => navigate('/login', { replace: true }), 1500);
    } catch (err) {
      handleFormError(err, { setError, notify });
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
          <Avatar sx={{ bgcolor: 'primary.main', width: 52, height: 52, mx: 'auto', mb: 1.5 }}>
            <LockResetIcon />
          </Avatar>
          <Typography variant="h5">Reset password</Typography>
        </Box>

        {!token ? (
          <Alert severity="error">The reset link is invalid or missing a token.</Alert>
        ) : done ? (
          <Alert severity="success">Your password has been reset. Redirecting to sign in...</Alert>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <FormTextField
              name="newPassword"
              control={control}
              label="New password"
              type="password"
              autoComplete="new-password"
              rules={rules.password()}
              helperText={PASSWORD_HINT}
            />
            <FormTextField
              name="confirmPassword"
              control={control}
              label="Confirm password"
              type="password"
              autoComplete="new-password"
              rules={{
                validate: (v) => (v ? v === getValues('newPassword') || 'Passwords do not match' : 'Confirm your password'),
              }}
            />
            <Button type="submit" variant="contained" size="large" fullWidth disabled={isSubmitting}>
              {isSubmitting ? 'Resetting...' : 'Reset password'}
            </Button>
          </form>
        )}
        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Link component={RouterLink} to="/login" variant="body2">
            Back to sign in
          </Link>
        </Box>
      </Paper>
    </Box>
  );
}

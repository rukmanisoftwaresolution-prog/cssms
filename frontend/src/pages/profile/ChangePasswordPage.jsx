import { useForm } from 'react-hook-form';
import { Button, Grid, Paper } from '@mui/material';
import { authApi } from '../../api/authApi';
import { useNotify } from '../../context/NotificationContext';
import PageHeader from '../../components/PageHeader';
import { FormTextField } from '../../components/FormFields';
import { handleFormError } from '../../utils/errorMessage';
import { PASSWORD_HINT, rules } from '../../utils/validators';

export default function ChangePasswordPage() {
  const notify = useNotify();
  const {
    control,
    handleSubmit,
    setError,
    reset,
    getValues,
    formState: { isSubmitting },
  } = useForm({ defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' } });

  const onSubmit = async ({ currentPassword, newPassword }) => {
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      notify.success('Password changed successfully');
      reset();
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  return (
    <>
      <PageHeader title="Change Password" subtitle="Use a strong password you do not use elsewhere" />
      <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, width: '100%', maxWidth: 480, minWidth: 0 }}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Grid container spacing={1}>
            <Grid item xs={12}>
              <FormTextField
                name="currentPassword"
                control={control}
                type="password"
                label="Current Password"
                autoComplete="current-password"
                rules={rules.required('Current password')}
              />
            </Grid>
            <Grid item xs={12}>
              <FormTextField
                name="newPassword"
                control={control}
                type="password"
                label="New Password"
                autoComplete="new-password"
                rules={rules.password()}
                helperText={PASSWORD_HINT}
              />
            </Grid>
            <Grid item xs={12}>
              <FormTextField
                name="confirmPassword"
                control={control}
                type="password"
                label="Confirm New Password"
                autoComplete="new-password"
                rules={{
                  validate: (v) => (v ? v === getValues('newPassword') || 'Passwords do not match' : 'Confirm your new password'),
                }}
              />
            </Grid>
            <Grid item xs={12}>
              <Button type="submit" variant="contained" disabled={isSubmitting} sx={{ width: { xs: '100%', sm: 'auto' } }}>
                {isSubmitting ? 'Updating...' : 'Change Password'}
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </>
  );
}

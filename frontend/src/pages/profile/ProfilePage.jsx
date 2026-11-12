import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button, Grid, Paper, Typography } from '@mui/material';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import { useNotify } from '../../context/NotificationContext';
import PageHeader from '../../components/PageHeader';
import { FormTextField } from '../../components/FormFields';
import StatusChip from '../../components/StatusChip';
import { handleFormError } from '../../utils/errorMessage';
import { rules } from '../../utils/validators';

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const notify = useNotify();
  const [saved, setSaved] = useState(false);
  const {
    control,
    handleSubmit,
    setError,
    reset,
    formState: { isSubmitting, isDirty },
  } = useForm({
    defaultValues: {
      fullName: user?.fullName || '',
      email: user?.email || '',
      mobileNumber: user?.mobileNumber || '',
    },
  });

  const onSubmit = async (values) => {
    try {
      const profile = await authApi.updateMe({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        mobileNumber: values.mobileNumber.trim(),
      });
      updateUser(profile);
      reset({ fullName: profile.fullName, email: profile.email, mobileNumber: profile.mobileNumber || '' });
      setSaved(true);
      notify.success('Profile updated successfully');
    } catch (err) {
      handleFormError(err, { setError, notify });
    }
  };

  return (
    <>
      <PageHeader title="My Profile" subtitle="Update your personal details" />
      <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 3 }, width: '100%', maxWidth: 720, minWidth: 0 }}>
        <Grid container spacing={2} sx={{ mb: 1 }} alignItems="center">
          <Grid item xs={12} sm={6}>
            <Typography variant="caption" color="text.secondary">
              Username
            </Typography>
            <Typography fontWeight={600}>{user?.username}</Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="caption" color="text.secondary" display="block">
              Role
            </Typography>
            <StatusChip status={user?.role} />
          </Grid>
        </Grid>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={12}>
              <FormTextField name="fullName" control={control} label="Full Name" required rules={rules.required('Full name')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormTextField name="email" control={control} label="Email" required rules={rules.email(true)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormTextField name="mobileNumber" control={control} label="Mobile Number" required rules={rules.mobile(true)} />
            </Grid>
            <Grid item xs={12}>
              <Button
                type="submit"
                variant="contained"
                disabled={isSubmitting || (!isDirty && saved)}
                sx={{ width: { xs: '100%', sm: 'auto' } }}
              >
                {isSubmitting ? 'Saving...' : 'Save Changes'}
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </>
  );
}

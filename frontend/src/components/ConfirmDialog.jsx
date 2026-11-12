import { useEffect, useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, TextField } from '@mui/material';

/** Confirmation dialog; set `inputLabel` to also collect a text (e.g. cancellation reason). */
export default function ConfirmDialog({
  open,
  title = 'Confirm',
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  color = 'primary',
  loading = false,
  inputLabel,
  onConfirm,
  onClose,
}) {
  const [text, setText] = useState('');
  useEffect(() => {
    if (open) setText('');
  }, [open]);

  return (
    <Dialog open={open} onClose={loading ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        {message && <DialogContentText component="div">{message}</DialogContentText>}
        {inputLabel && (
          <TextField
            autoFocus
            multiline
            minRows={2}
            label={inputLabel}
            value={text}
            onChange={(e) => setText(e.target.value)}
            sx={{ mt: 2 }}
          />
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={loading} color="inherit">
          {cancelText}
        </Button>
        <Button onClick={() => onConfirm?.(text)} disabled={loading} color={color} variant="contained">
          {loading ? 'Please wait...' : confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

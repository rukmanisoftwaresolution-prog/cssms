import { Box, Button, MenuItem, Paper, TextField } from '@mui/material';
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff';

/** Layout wrapper for filter controls */
export default function FilterBar({ children, onReset, sx }) {
  return (
    <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2 }, mb: 2, width: '100%', minWidth: 0, ...sx }}>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: { xs: 1, sm: 2 },
          alignItems: { xs: 'stretch', sm: 'center' },
          minWidth: 0,
          '& > .MuiFormControl-root, & > .MuiButton-root': { width: { xs: '100%', sm: 'auto' }, minWidth: 0 },
        }}
      >
        {children}
        {onReset && (
          <Button startIcon={<FilterAltOffIcon />} onClick={onReset} color="inherit">
            Reset
          </Button>
        )}
      </Box>
    </Paper>
  );
}

/** Uncontrolled-by-form select used in filter bars. options: [{value,label}] */
export function FilterSelect({ label, value, onChange, options = [], allLabel = 'All', minWidth = 160, loading }) {
  return (
    <TextField
      select
      label={label}
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value)}
      sx={{ minWidth: { xs: 0, sm: minWidth }, width: { xs: '100%', sm: 'auto' }, maxWidth: '100%' }}
      InputLabelProps={{ shrink: true }}
      SelectProps={{ displayEmpty: true }}
      disabled={loading}
    >
      <MenuItem value="">{allLabel}</MenuItem>
      {options.map((o) => (
        <MenuItem key={o.value} value={o.value}>
          {o.label}
        </MenuItem>
      ))}
    </TextField>
  );
}

export function FilterDate({ label, value, onChange, minWidth = 150 }) {
  return (
    <TextField
      type="date"
      label={label}
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value)}
      InputLabelProps={{ shrink: true }}
      sx={{ minWidth: { xs: 0, sm: minWidth }, width: { xs: '100%', sm: 'auto' }, maxWidth: '100%' }}
    />
  );
}

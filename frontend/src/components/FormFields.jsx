import { Controller } from 'react-hook-form';
import { FormControlLabel, MenuItem, Switch, TextField } from '@mui/material';

/** Text/number/date input bound to react-hook-form */
export function FormTextField({ name, control, label, rules, type = 'text', helperText, inputProps, ...rest }) {
  return (
    <Controller
      name={name}
      control={control}
      rules={rules}
      render={({ field, fieldState }) => (
        <TextField
          {...field}
          value={field.value ?? ''}
          type={type}
          label={label}
          error={!!fieldState.error}
          helperText={fieldState.error?.message || (helperText === null ? undefined : helperText || ' ')}
          inputProps={{ ...(type === 'number' ? { step: 'any', min: 0 } : {}), ...inputProps }}
          InputLabelProps={type === 'date' || type === 'month' ? { shrink: true } : undefined}
          {...rest}
        />
      )}
    />
  );
}

/** Native date input (yyyy-MM-dd) bound to react-hook-form */
export function FormDatePicker(props) {
  return <FormTextField {...props} type="date" />;
}

/** options: [{value,label}] */
export function FormSelect({ name, control, label, rules, options = [], helperText, emptyLabel, loading, disabled, ...rest }) {
  return (
    <Controller
      name={name}
      control={control}
      rules={rules}
      render={({ field, fieldState }) => (
        <TextField
          {...field}
          select
          value={field.value ?? ''}
          label={label}
          error={!!fieldState.error}
          helperText={fieldState.error?.message || (helperText === null ? undefined : helperText || ' ')}
          disabled={loading || disabled}
          {...rest}
        >
          {emptyLabel !== undefined && (
            <MenuItem value="">
              <em>{emptyLabel}</em>
            </MenuItem>
          )}
          {options.map((o) => (
            <MenuItem key={o.value} value={o.value}>
              {o.label}
            </MenuItem>
          ))}
        </TextField>
      )}
    />
  );
}

export function FormSwitch({ name, control, label }) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <FormControlLabel
          control={<Switch checked={!!field.value} onChange={(e) => field.onChange(e.target.checked)} />}
          label={label}
        />
      )}
    />
  );
}

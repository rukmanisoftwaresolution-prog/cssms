import { useEffect, useRef, useState } from 'react';
import { IconButton, InputAdornment, TextField } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

/** Debounced search input */
export default function SearchBar({ value = '', onChange, placeholder = 'Search...', delay = 400, sx }) {
  const [text, setText] = useState(value);
  const first = useRef(true);

  useEffect(() => {
    setText(value);
  }, [value]);

  useEffect(() => {
    if (first.current) {
      first.current = false;
      return undefined;
    }
    if (text === value) return undefined;
    const t = setTimeout(() => onChange?.(text), delay);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);

  return (
    <TextField
      value={text}
      onChange={(e) => setText(e.target.value)}
      placeholder={placeholder}
      sx={{ width: { xs: '100%', sm: 'auto' }, minWidth: { xs: 0, sm: 220 }, maxWidth: '100%', ...sx }}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon fontSize="small" />
          </InputAdornment>
        ),
        endAdornment: text ? (
          <InputAdornment position="end">
            <IconButton
              size="small"
              aria-label="clear search"
              onClick={() => {
                setText('');
                onChange?.('');
              }}
            >
              <ClearIcon fontSize="small" />
            </IconButton>
          </InputAdornment>
        ) : null,
      }}
    />
  );
}

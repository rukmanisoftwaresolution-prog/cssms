import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  breakpoints: {
    values: { xs: 0, sm: 768, md: 1024, lg: 1441, xl: 1920 },
  },
  palette: {
    primary: { main: '#1b4965', light: '#4c7a96', dark: '#0f2f45' },
    secondary: { main: '#e07a1f' },
    success: { main: '#2e7d32' },
    background: { default: '#f4f6f8', paper: '#ffffff' },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 700,
      fontSize: '2.25rem',
      '@media (max-width:767.95px)': { fontSize: '1.25rem' },
      '@media (min-width:768px) and (max-width:1440.95px)': { fontSize: '1.875rem' },
    },
    h5: {
      fontWeight: 700,
      fontSize: '2rem',
      '@media (max-width:767.95px)': { fontSize: '1.125rem' },
      '@media (min-width:768px) and (max-width:1440.95px)': { fontSize: '1.25rem' },
    },
    h6: {
      fontWeight: 600,
      fontSize: '1.25rem',
      '@media (max-width:767.95px)': { fontSize: '1.125rem' },
    },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { minHeight: 44, whiteSpace: 'normal' } },
    },
    MuiIconButton: { styleOverrides: { root: { minWidth: 44, minHeight: 44 } } },
    MuiToggleButton: { styleOverrides: { root: { minWidth: 44, minHeight: 44 } } },
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true, variant: 'outlined' } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: { root: { border: '1px solid #e3e8ee', minWidth: 0, maxWidth: '100%' } },
    },
    MuiFormControl: { styleOverrides: { root: { minWidth: 0, maxWidth: '100%' } } },
    MuiDialog: {
      styleOverrides: {
        paper: {
          width: '100%',
          maxWidth: 'calc(100% - 24px)',
          margin: 12,
          '@media (min-width:768px)': { margin: 24 },
        },
        paperFullWidth: {
          width: 'calc(100% - 24px)',
          '@media (min-width:768px)': { width: '70vw' },
        },
        paperWidthXs: { maxWidth: 'min(95vw, 444px)' },
        paperWidthSm: { '@media (min-width:768px)': { maxWidth: 'min(70vw, 600px)' } },
        paperWidthMd: { '@media (min-width:768px)': { maxWidth: 'min(70vw, 900px)' } },
        paperWidthLg: { '@media (min-width:768px)': { maxWidth: 'min(70vw, 1200px)' } },
        paperWidthXl: { '@media (min-width:768px)': { maxWidth: 'min(70vw, 1536px)' } },
      },
    },
    MuiDialogContent: { styleOverrides: { root: { minWidth: 0, overflowX: 'auto' } } },
    MuiDialogActions: {
      styleOverrides: {
        root: {
          '@media (max-width:767.95px)': {
            alignItems: 'stretch',
            flexDirection: 'column-reverse',
            '& > :not(style) ~ :not(style)': { marginLeft: 0 },
            '& .MuiButton-root': { width: '100%' },
          },
        },
      },
    },
    MuiTableContainer: { styleOverrides: { root: { width: '100%', maxWidth: '100%', overflowX: 'auto' } } },
    MuiTablePagination: {
      styleOverrides: {
        toolbar: { flexWrap: 'wrap', minHeight: 56, rowGap: 4, paddingLeft: 12, paddingRight: 12 },
        displayedRows: { marginLeft: 8 },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: { fontWeight: 600, backgroundColor: '#eef2f6', whiteSpace: 'nowrap' },
      },
    },
    MuiDialogTitle: { styleOverrides: { root: { fontWeight: 600 } } },
  },
});

export default theme;

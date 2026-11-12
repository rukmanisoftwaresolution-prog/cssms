import { useState } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import TableViewIcon from '@mui/icons-material/TableView';
import { reportApi } from '../api/reportApi';
import { useNotify } from '../context/NotificationContext';
import { formatByType } from '../utils/formatters';
import { getErrorMessage } from '../utils/errorMessage';
import { saveResponse } from '../utils/download';
import LoadingOverlay from './LoadingOverlay';

const isNumeric = (type) => type === 'NUMBER' || type === 'CURRENCY';

/**
 * Generic renderer for the backend ReportResponse { title, subtitle, columns, rows, summary }.
 * Pass `exportKey` + `exportParams` to enable PDF/Excel export buttons.
 */
export default function ReportTable({ report, loading = false, exportKey, exportParams, emptyText = 'No data found' }) {
  const notify = useNotify();
  const [exporting, setExporting] = useState(null);

  const handleExport = async (format) => {
    setExporting(format);
    try {
      const res = await reportApi.export(exportKey, format, exportParams);
      saveResponse(res, `${exportKey}.${format}`);
    } catch (err) {
      notify.error(getErrorMessage(err, 'Export failed'));
    } finally {
      setExporting(null);
    }
  };

  const columns = report?.columns || [];
  const rows = report?.rows || [];
  const summary = report?.summary || [];

  return (
    <Paper variant="outlined" sx={{ position: 'relative', minHeight: 160, width: '100%', minWidth: 0 }}>
      <LoadingOverlay open={loading} />
      {!report ? (
        <Box sx={{ p: { xs: 3, sm: 6 }, textAlign: 'center', color: 'text.secondary' }}>
          <Typography variant="body2">Select filters and click “Generate” to view the report.</Typography>
        </Box>
      ) : (
        <>
          <Box
            sx={{
              p: 2,
              display: 'flex',
              flexWrap: 'wrap',
              flexDirection: { xs: 'column', sm: 'row' },
              gap: 2,
              justifyContent: 'space-between',
              alignItems: 'center',
              minWidth: 0,
              borderBottom: '1px solid',
              borderColor: 'divider',
            }}
          >
            <Box sx={{ minWidth: 0, width: { xs: '100%', sm: 'auto' } }}>
              <Typography variant="h6">{report.title}</Typography>
              {report.subtitle && (
                <Typography variant="body2" color="text.secondary">
                  {report.subtitle}
                </Typography>
              )}
            </Box>
            {exportKey && (
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: { xs: 'column', sm: 'row' },
                  gap: 1,
                  width: { xs: '100%', sm: 'auto' },
                  '& .MuiButton-root': { width: { xs: '100%', sm: 'auto' } },
                }}
              >
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={exporting === 'pdf' ? <CircularProgress size={16} /> : <PictureAsPdfIcon />}
                  disabled={!!exporting}
                  onClick={() => handleExport('pdf')}
                >
                  Export PDF
                </Button>
                <Button
                  variant="outlined"
                  color="success"
                  startIcon={exporting === 'xlsx' ? <CircularProgress size={16} /> : <TableViewIcon />}
                  disabled={!!exporting}
                  onClick={() => handleExport('xlsx')}
                >
                  Export Excel
                </Button>
              </Box>
            )}
          </Box>

          {summary.length > 0 && (
            <Grid container spacing={2} sx={{ p: 2 }}>
              {summary.map((s, i) => (
                <Grid item xs={12} sm={6} md={3} key={`${s.label}-${i}`}>
                  <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'action.hover' }}>
                    <Typography variant="caption" color="text.secondary">
                      {s.label}
                    </Typography>
                    <Typography variant="subtitle1" fontWeight={700}>
                      {formatByType(s.value, s.type)}
                    </Typography>
                  </Box>
                </Grid>
              ))}
            </Grid>
          )}

          <TableContainer sx={{ maxHeight: 560, width: '100%', maxWidth: '100%', minWidth: 0, overflowX: 'auto' }}>
            <Table stickyHeader size="small" sx={{ minWidth: '100%', width: 'max-content' }}>
              <TableHead>
                <TableRow>
                  {columns.map((c) => (
                    <TableCell key={c.key} align={isNumeric(c.type) ? 'right' : 'left'}>
                      {c.label}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={Math.max(columns.length, 1)} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                      {emptyText}
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row, i) => (
                    <TableRow hover key={i}>
                      {columns.map((c) => (
                        <TableCell key={c.key} align={isNumeric(c.type) ? 'right' : 'left'}>
                          {formatByType(row[c.key], c.type)}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Paper>
  );
}

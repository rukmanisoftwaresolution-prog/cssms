import {
  Box,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  Typography,
} from '@mui/material';
import InboxIcon from '@mui/icons-material/Inbox';
import { PAGE_SIZE_OPTIONS } from '../utils/constants';

/**
 * Server-side paginated + sortable table.
 * columns: [{ key, label, sortable, align, width, render(row, index) }]
 * `table` is the object returned by useServerTable (optional shortcut).
 */
export default function DataTable({
  columns,
  rows = [],
  loading = false,
  page = 0,
  size = 10,
  totalElements = 0,
  sort,
  onPageChange,
  onSizeChange,
  onSort,
  actions,
  actionsLabel = 'Actions',
  rowKey = 'id',
  emptyText = 'No records found',
  pagination = true,
  dense = false,
  rowSx,
  table,
}) {
  if (table) {
    ({ rows, loading, page, size, totalElements, sort } = table);
    onPageChange = table.setPage;
    onSizeChange = table.setSize;
    onSort = table.toggleSort;
  }
  const colCount = columns.length + (actions ? 1 : 0);

  return (
    <Paper variant="outlined" sx={{ overflow: 'hidden', position: 'relative', width: '100%', minWidth: 0 }}>
      <TableContainer sx={{ width: '100%', maxWidth: '100%', minWidth: 0, overflowX: 'auto', maxHeight: { md: 'calc(100vh - 290px)' } }}>
        <Table stickyHeader size={dense ? 'small' : 'medium'} sx={{ minWidth: '100%', width: 'max-content' }}>
          <TableHead>
            <TableRow>
              {columns.map((c) => (
                <TableCell
                  key={c.key}
                  align={c.align || 'left'}
                  sx={{ width: c.width, minWidth: c.minWidth }}
                  sortDirection={sort?.field === (c.sortKey || c.key) ? sort.direction : false}
                >
                  {c.sortable && onSort ? (
                    <TableSortLabel
                      active={sort?.field === (c.sortKey || c.key)}
                      direction={sort?.field === (c.sortKey || c.key) ? sort.direction : 'asc'}
                      onClick={() => onSort(c.sortKey || c.key)}
                    >
                      {c.label}
                    </TableSortLabel>
                  ) : (
                    c.label
                  )}
                </TableCell>
              ))}
              {actions && (
                <TableCell align="right" sx={{ width: 1 }}>
                  {actionsLabel}
                </TableCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={colCount} align="center" sx={{ py: 8 }}>
                  <CircularProgress size={32} />
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={colCount} align="center" sx={{ py: 8, color: 'text.secondary' }}>
                  <InboxIcon sx={{ fontSize: 40, opacity: 0.5 }} />
                  <Typography variant="body2">{emptyText}</Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row, idx) => (
                <TableRow
                  hover
                  key={row[rowKey] ?? idx}
                  sx={{ opacity: loading ? 0.6 : 1, ...(typeof rowSx === 'function' ? rowSx(row) : rowSx) }}
                >
                  {columns.map((c) => (
                    <TableCell key={c.key} align={c.align || 'left'}>
                      {c.render ? c.render(row, idx) : (row[c.key] ?? '-')}
                    </TableCell>
                  ))}
                  {actions && (
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Box sx={{ display: 'inline-flex' }}>{actions(row)}</Box>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {pagination && (
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={PAGE_SIZE_OPTIONS}
          onPageChange={(_, p) => onPageChange?.(p)}
          onRowsPerPageChange={(e) => onSizeChange?.(parseInt(e.target.value, 10))}
        />
      )}
    </Paper>
  );
}

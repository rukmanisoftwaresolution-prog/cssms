import { useCallback, useEffect, useRef, useState } from 'react';
import { useNotify } from '../context/NotificationContext';
import { getErrorMessage } from '../utils/errorMessage';

const EMPTY = { content: [], totalElements: 0, page: 0, size: 10, totalPages: 0 };

/**
 * Server-side paginated/sorted/filtered list state.
 * `fetcher(params)` must return the backend page response.
 */
export default function useServerTable(fetcher, { initialFilters = {}, initialSort = null, initialSize = 10 } = {}) {
  const notify = useNotify();
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(initialSize);
  const [sort, setSort] = useState(initialSort);
  const [data, setData] = useState(EMPTY);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetcherRef
      .current({ ...filters, page, size, sort: sort ? `${sort.field},${sort.direction}` : undefined })
      .then((res) => {
        if (!cancelled) setData(res || EMPTY);
      })
      .catch((err) => {
        if (!cancelled) {
          setData(EMPTY);
          notify.error(getErrorMessage(err, 'Failed to load data'));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters, page, size, sort, tick, notify]);

  const setFilter = useCallback((name, value) => {
    setFilters((f) => ({ ...f, [name]: value }));
    setPage(0);
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(initialFilters);
    setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const changeSize = useCallback((s) => {
    setSize(s);
    setPage(0);
  }, []);

  const toggleSort = useCallback((field) => {
    setSort((cur) => {
      if (!cur || cur.field !== field) return { field, direction: 'asc' };
      return { field, direction: cur.direction === 'asc' ? 'desc' : 'asc' };
    });
    setPage(0);
  }, []);

  const refresh = useCallback(() => setTick((t) => t + 1), []);

  return {
    rows: data.content || [],
    totalElements: data.totalElements ?? 0,
    loading,
    page,
    size,
    sort,
    filters,
    setPage,
    setSize: changeSize,
    setFilter,
    resetFilters,
    toggleSort,
    refresh,
  };
}

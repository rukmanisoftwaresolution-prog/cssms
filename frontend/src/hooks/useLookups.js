import { useEffect, useState } from 'react';
import { supplierApi } from '../api/supplierApi';
import { customerApi } from '../api/customerApi';
import { materialApi } from '../api/materialApi';
import { useNotify } from '../context/NotificationContext';
import { getErrorMessage } from '../utils/errorMessage';
import { LOOKUP_SIZE } from '../utils/constants';

function useLookup(fetcher, params, key, enabled = true) {
  const notify = useNotify();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    fetcher({ size: LOOKUP_SIZE, ...params })
      .then((res) => {
        if (!cancelled) setItems(res?.content || []);
      })
      .catch((err) => {
        if (!cancelled) notify.error(getErrorMessage(err, 'Failed to load dropdown data'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, enabled]);

  return { items, loading };
}

/** options: [{value,label,raw}] */
const toOptions = (items, valueKey, labelFn) =>
  items.map((i) => ({ value: i[valueKey], label: labelFn(i), raw: i }));

export function useSuppliers(activeOnly = true, enabled = true) {
  const { items, loading } = useLookup(supplierApi.list, activeOnly ? { status: 'ACTIVE' } : {}, `s${activeOnly}`, enabled);
  return { items, loading, options: toOptions(items, 'id', (s) => `${s.supplierName} (${s.supplierCode})`) };
}

export function useCustomers(activeOnly = true, enabled = true) {
  const { items, loading } = useLookup(customerApi.list, activeOnly ? { status: 'ACTIVE' } : {}, `c${activeOnly}`, enabled);
  return {
    items,
    loading,
    options: toOptions(items, 'id', (c) => `${c.customerName}${c.companyName ? ` - ${c.companyName}` : ''} (${c.customerCode})`),
  };
}

export function useMaterials(enabled = true) {
  const { items, loading } = useLookup(materialApi.list, {}, 'm', enabled);
  return { items, loading, options: toOptions(items, 'id', (m) => `${m.materialName} (${m.unit || ''})`) };
}

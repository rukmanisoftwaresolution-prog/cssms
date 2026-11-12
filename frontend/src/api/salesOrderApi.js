import axiosClient from './axiosClient';
import { createCrudApi } from './crudApi';

export const salesOrderApi = {
  ...createCrudApi('/sales-orders'),
  cancel: (id, reason) => axiosClient.post(`/sales-orders/${id}/cancel`, { reason }).then((r) => r.data),
  invoice: (id) => axiosClient.get(`/sales-orders/${id}/invoice`, { responseType: 'blob' }),
};

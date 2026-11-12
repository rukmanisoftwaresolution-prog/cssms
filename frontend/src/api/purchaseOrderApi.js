import axiosClient from './axiosClient';
import { createCrudApi } from './crudApi';

export const purchaseOrderApi = {
  ...createCrudApi('/purchase-orders'),
  cancel: (id, reason) => axiosClient.post(`/purchase-orders/${id}/cancel`, { reason }).then((r) => r.data),
};

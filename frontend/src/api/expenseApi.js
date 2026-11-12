import axiosClient from './axiosClient';
import { createCrudApi } from './crudApi';

export const expenseApi = {
  ...createCrudApi('/expenses'),
  uploadAttachment: (id, file) => {
    const form = new FormData();
    form.append('file', file);
    return axiosClient.post(`/expenses/${id}/attachment`, form).then((r) => r.data);
  },
  downloadAttachment: (id) => axiosClient.get(`/expenses/${id}/attachment`, { responseType: 'blob' }),
  removeAttachment: (id) => axiosClient.delete(`/expenses/${id}/attachment`),
};

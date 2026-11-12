import axiosClient from './axiosClient';

export const inventoryApi = {
  list: (params) => axiosClient.get('/inventory', { params }).then((r) => r.data),
};

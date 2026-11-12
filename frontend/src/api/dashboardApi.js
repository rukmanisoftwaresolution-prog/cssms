import axiosClient from './axiosClient';

export const dashboardApi = {
  get: () => axiosClient.get('/dashboard').then((r) => r.data),
};

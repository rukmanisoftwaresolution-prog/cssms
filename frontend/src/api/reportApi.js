import axiosClient from './axiosClient';

export const reportApi = {
  get: (key, params) => axiosClient.get(`/reports/${key}`, { params }).then((r) => r.data),
  export: (key, format, params) =>
    axiosClient.get(`/reports/${key}/export`, { params: { ...params, format }, responseType: 'blob' }),
};

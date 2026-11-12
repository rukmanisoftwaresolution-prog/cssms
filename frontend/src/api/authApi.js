import axiosClient from './axiosClient';

export const authApi = {
  login: (username, password) => axiosClient.post('/auth/login', { username, password }).then((r) => r.data),
  refresh: (refreshToken) => axiosClient.post('/auth/refresh', { refreshToken }).then((r) => r.data),
  logout: (refreshToken) => axiosClient.post('/auth/logout', { refreshToken }),
  changePassword: (body) => axiosClient.post('/auth/change-password', body),
  forgotPassword: (email) => axiosClient.post('/auth/forgot-password', { email }).then((r) => r.data),
  resetPassword: (token, newPassword) => axiosClient.post('/auth/reset-password', { token, newPassword }),
  me: () => axiosClient.get('/auth/me').then((r) => r.data),
  updateMe: (body) => axiosClient.put('/auth/me', body).then((r) => r.data),
};

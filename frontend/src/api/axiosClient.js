import axios from 'axios';
import { tokenStorage } from '../utils/tokenStorage';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
export const AUTH_LOGOUT_EVENT = 'cssms:auth-logout';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
});

const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/refresh', '/auth/forgot-password', '/auth/reset-password'];
const isPublicAuthCall = (url = '') => PUBLIC_AUTH_PATHS.some((p) => url.startsWith(p));

/** Drop empty params so filters like `status=''` are not sent */
function cleanParams(params) {
  if (!params) return params;
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  );
}

axiosClient.interceptors.request.use((config) => {
  config.params = cleanParams(config.params);
  const token = tokenStorage.getAccessToken();
  if (token && !isPublicAuthCall(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
  return config;
});

let refreshPromise = null;

/** Performs a single in-flight refresh; concurrent callers share the same promise. */
export function refreshAccessToken() {
  if (refreshPromise) return refreshPromise;
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return Promise.reject(new Error('No refresh token'));

  refreshPromise = axios
    .post(`${API_BASE_URL}/auth/refresh`, { refreshToken }, { headers: { 'Content-Type': 'application/json' } })
    .then((res) => {
      const data = res.data;
      tokenStorage.setTokens(data.accessToken, data.refreshToken);
      if (data.user) tokenStorage.setUser(data.user);
      return data;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

function forceLogout() {
  tokenStorage.clear();
  window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT));
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign('/login');
  }
}

/** Blob error bodies (failed downloads) are parsed back into JSON so messages are available */
async function normalizeBlobError(error) {
  const data = error?.response?.data;
  if (data instanceof Blob && data.type?.includes('json')) {
    try {
      error.response.data = JSON.parse(await data.text());
    } catch {
      /* keep blob */
    }
  }
  return error;
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    await normalizeBlobError(error);
    const original = error.config;
    const status = error.response?.status;

    if (status !== 401 || !original || original._retry || isPublicAuthCall(original.url)) {
      return Promise.reject(error);
    }
    if (!tokenStorage.getRefreshToken()) {
      forceLogout();
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      const data = await refreshAccessToken();
      original.headers.Authorization = `Bearer ${data.accessToken}`;
      return axiosClient(original);
    } catch (refreshError) {
      forceLogout();
      return Promise.reject(error);
    }
  }
);

export default axiosClient;

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { AUTH_LOGOUT_EVENT, refreshAccessToken } from '../api/axiosClient';
import { tokenStorage } from '../utils/tokenStorage';
import { can as canRole } from '../utils/constants';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (tokenStorage.getAccessToken() ? tokenStorage.getUser() : null));
  const [initializing, setInitializing] = useState(() => !!tokenStorage.getAccessToken());

  const clearSession = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  // Validate stored session on startup
  useEffect(() => {
    let cancelled = false;
    if (!tokenStorage.getAccessToken()) {
      setInitializing(false);
      return undefined;
    }
    authApi
      .me()
      .then((profile) => {
        if (cancelled) return;
        tokenStorage.setUser(profile);
        setUser(profile);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err?.response?.status === 401 || !tokenStorage.getAccessToken()) clearSession();
      })
      .finally(() => {
        if (!cancelled) setInitializing(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  // Axios client signals unrecoverable auth failure
  useEffect(() => {
    const handler = () => setUser(null);
    window.addEventListener(AUTH_LOGOUT_EVENT, handler);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, handler);
  }, []);

  const login = useCallback(async (username, password) => {
    const data = await authApi.login(username, password);
    tokenStorage.setTokens(data.accessToken, data.refreshToken);
    tokenStorage.setUser(data.user);
    setUser(data.user);
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      /* ignore - local logout always succeeds */
    }
    clearSession();
  }, [clearSession]);

  const refresh = useCallback(async () => {
    const data = await refreshAccessToken();
    if (data.user) setUser(data.user);
    return data;
  }, []);

  const updateUser = useCallback((profile) => {
    tokenStorage.setUser(profile);
    setUser(profile);
  }, []);

  const value = useMemo(() => {
    const role = user?.role ?? null;
    return {
      user,
      role,
      initializing,
      isAuthenticated: !!user,
      isAdmin: role === 'ADMIN',
      login,
      logout,
      refresh,
      updateUser,
      hasRole: (...roles) => !!role && roles.flat().includes(role),
      can: (resource, action = 'view') => canRole(role, resource, action),
    };
  }, [user, initializing, login, logout, refresh, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

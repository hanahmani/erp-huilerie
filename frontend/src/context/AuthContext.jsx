import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { tokenStore } from '../services/api';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(tokenStore.get()));

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  // Au chargement : si un jeton existe, on récupère l'utilisateur (et ses permissions à jour).
  useEffect(() => {
    if (!tokenStore.get()) return;
    authService.me()
      .then(setUser)
      .catch(logout)
      .finally(() => setLoading(false));
  }, [logout]);

  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener('auth:expired', onExpired);
    return () => window.removeEventListener('auth:expired', onExpired);
  }, []);

  const login = useCallback(async (username, password) => {
    const res = await authService.login(username, password);
    tokenStore.set(res.token);
    setUser(res.user);
    return res.user;
  }, []);

  const changePassword = useCallback(async (current, next) => {
    const res = await authService.changePassword(current, next);
    tokenStore.set(res.token);
    setUser(res.user);
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    login,
    logout,
    changePassword,
    // une permission, ou une liste dont au moins une suffit
    can: (permission) => !permission || (Array.isArray(permission)
      ? permission.some((p) => user?.permissions?.includes(p))
      : Boolean(user?.permissions?.includes(permission))),
  }), [user, loading, login, logout, changePassword]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}

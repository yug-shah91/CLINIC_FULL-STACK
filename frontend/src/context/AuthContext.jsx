import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { STORAGE_KEY } from '../api/axios';
import { authApi } from '../api/services';
import { useToast } from './ToastContext';

const AuthContext = createContext(null);

function readSavedUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const toast = useToast();
  // Start from localStorage so a page refresh does not log the user out.
  const [user, setUser] = useState(readSavedUser);
  // While true we do not trust the saved session yet (it is being verified with the backend).
  const [checking, setChecking] = useState(() => Boolean(readSavedUser()?.token));

  const saveSession = (res) => {
    const session = { token: res.token, name: res.name, email: res.email, role: res.role };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    setUser(session);
    return session;
  };

  const login = async (credentials) => saveSession(await authApi.login(credentials));
  const register = async (details) => saveSession(await authApi.register(details));

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
  }, []);

  // On load, ask the backend who this token really belongs to and use ITS role, not the saved one.
  useEffect(() => {
    const saved = readSavedUser();
    if (!saved?.token) return;
    let cancelled = false;
    authApi.me()
      .then((me) => {
        if (cancelled) return;
        const session = { token: saved.token, name: me.name, email: me.email, role: me.role };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        setUser(session);
      })
      .catch((err) => {
        if (cancelled) return;
        // 401 is handled by the "auth:expired" listener below. For other failures
        // (server down) keep the saved session so a refresh does not log people out.
        if (err.status === 401) { localStorage.removeItem(STORAGE_KEY); setUser(null); }
      })
      .finally(() => { if (!cancelled) setChecking(false); });
    return () => { cancelled = true; };
  }, []);

  // The Axios interceptor fires "auth:expired" when the token is rejected.
  useEffect(() => {
    const onExpired = () => {
      if (localStorage.getItem(STORAGE_KEY)) {
        logout();
        toast.error('Your session expired. Please log in again.');
      }
    };
    window.addEventListener('auth:expired', onExpired);
    return () => window.removeEventListener('auth:expired', onExpired);
  }, [logout, toast]);

  const value = useMemo(
    () => ({ user, checking, isDoctor: user?.role === 'ADMIN', login, register, logout }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [user, checking, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

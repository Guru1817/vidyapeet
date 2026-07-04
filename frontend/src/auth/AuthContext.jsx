import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api, { setAuthToken } from '../api/client';
import { setTenantSlug } from '../lib/tenant';

const AuthContext = createContext(null);

function loadStoredUser() {
  try {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadStoredUser);
  const [ready, setReady] = useState(false);

  // On first load, if we have a token, refresh the profile to confirm validity.
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setReady(true);
      return;
    }
    api
      .get('/api/auth/me')
      .then((res) => persistUser(res.data))
      .catch(() => clearSession())
      .finally(() => setReady(true));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function persistUser(u) {
    setUser(u);
    localStorage.setItem('user', JSON.stringify(u));
    if (u?.instituteSlug) {
      setTenantSlug(u.instituteSlug);
    }
  }

  function clearSession() {
    setUser(null);
    setAuthToken(null);
    localStorage.removeItem('user');
  }

  async function login({ slug, email, password }) {
    const res = await api.post('/api/auth/login', { slug: slug || null, email, password });
    setAuthToken(res.data.token);
    persistUser(res.data.user);
    return res.data.user;
  }

  async function registerStudent({ slug, name, email, password }) {
    const res = await api.post('/api/auth/register', { slug, name, email, password });
    setAuthToken(res.data.token);
    persistUser(res.data.user);
    return res.data.user;
  }

  function logout() {
    clearSession();
  }

  const value = useMemo(
    () => ({ user, ready, login, registerStudent, logout }),
    [user, ready],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';
import { useAuth } from '../auth/AuthContext';

const ThemeContext = createContext(null);

const LIGHT = 'light';
const DARK = 'dark';

// Normalize any theme representation (server enum "LIGHT"/"DARK", stored
// "light"/"dark") down to our internal lowercase value, or null if unknown.
function normalize(value) {
  if (typeof value !== 'string') return null;
  const v = value.toLowerCase();
  return v === LIGHT || v === DARK ? v : null;
}

// Reads the guest preference persisted client-side.
function storedTheme() {
  try {
    return normalize(localStorage.getItem('theme'));
  } catch {
    return null;
  }
}

// Falls back to the OS/browser appearance when nothing is stored.
function systemTheme() {
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? DARK : LIGHT;
  }
  return LIGHT;
}

// Resolution order (Req 4.2, 4.3, 4.6): a logged-in user's persisted
// themePreference wins, then the guest's localStorage choice, then the
// prefers-color-scheme media query.
function resolveTheme(user) {
  const fromUser = normalize(user?.themePreference);
  if (fromUser) return fromUser;
  const fromStorage = storedTheme();
  if (fromStorage) return fromStorage;
  return systemTheme();
}

// Toggle the Tailwind `dark` class on <html>. This never touches the
// --brand/--brand-dark CSS variables, so institute brand theming is preserved.
function applyThemeClass(theme) {
  const root = document.documentElement;
  if (theme === DARK) {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
}

export function ThemeProvider({ children }) {
  const { user } = useAuth();
  const [theme, setThemeState] = useState(() => resolveTheme(user));

  // Re-resolve whenever the auth state changes (login/logout, profile refresh)
  // so a logged-in user's server preference is applied across devices (Req 4.6).
  useEffect(() => {
    setThemeState(resolveTheme(user));
  }, [user]);

  // Keep the <html> class in sync with the active theme.
  useEffect(() => {
    applyThemeClass(theme);
  }, [theme]);

  // Set an explicit theme. Guests persist to localStorage; logged-in users also
  // persist server-side via the Theme_Service (Req 4.2, 4.4).
  const setTheme = useCallback(
    (next) => {
      const resolved = normalize(next) || LIGHT;
      setThemeState(resolved);
      try {
        localStorage.setItem('theme', resolved);
      } catch {
        // Ignore storage failures (e.g. private mode); the in-memory state still applies.
      }
      if (user) {
        api.put('/api/auth/me/theme', { theme: resolved.toUpperCase() }).catch(() => {
          // Best-effort: the local choice already took effect; a failed sync is non-fatal.
        });
      }
    },
    [user],
  );

  const toggleTheme = useCallback(() => {
    setTheme(theme === DARK ? LIGHT : DARK);
  }, [theme, setTheme]);

  const value = useMemo(
    () => ({ theme, isDark: theme === DARK, setTheme, toggleTheme }),
    [theme, setTheme, toggleTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return ctx;
}

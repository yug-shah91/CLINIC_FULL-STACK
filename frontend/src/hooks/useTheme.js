import { useCallback, useEffect, useState } from 'react';

const KEY = 'clinic_theme';
const DARK_QUERY = '(prefers-color-scheme: dark)';

function savedTheme() {
  try {
    const saved = localStorage.getItem(KEY);
    if (saved === 'light' || saved === 'dark') return saved;
  } catch { /* storage blocked: treat as "no saved choice" */ }
  return null;
}

// A saved choice wins; otherwise follow the operating system. Only a harmless
// "light"/"dark" preference is stored, never anything about the user.
export function initialTheme() {
  return savedTheme() ?? (window.matchMedia?.(DARK_QUERY).matches ? 'dark' : 'light');
}

export const applyTheme = (theme) => { document.documentElement.dataset.theme = theme; };

export default function useTheme() {
  const [theme, setTheme] = useState(initialTheme);

  useEffect(() => { applyTheme(theme); }, [theme]);

  // While the user has not picked a theme themselves, follow live OS changes
  // (for example the system switching to dark at sunset).
  useEffect(() => {
    const query = window.matchMedia?.(DARK_QUERY);
    if (!query) return undefined;
    const onChange = (event) => {
      if (savedTheme() === null) setTheme(event.matches ? 'dark' : 'light');
    };
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const toggle = useCallback(() => {
    setTheme((current) => {
      const next = current === 'dark' ? 'light' : 'dark';
      try { localStorage.setItem(KEY, next); } catch { /* ignore */ }
      return next;
    });
  }, []);

  return { theme, toggle };
}

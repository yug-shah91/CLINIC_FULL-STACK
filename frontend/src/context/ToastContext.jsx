import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), []);

  const add = useCallback(
    (type, message) => {
      const id = Date.now() + Math.random();
      setToasts((list) => [...list, { id, type, message }]);
      setTimeout(() => remove(id), 4000);
    },
    [remove]
  );

  // useMemo keeps this object the same between renders, so components that
  // list `toast` in a useEffect dependency array don't re-run needlessly.
  const toast = useMemo(
    () => ({ success: (m) => add('success', m), error: (m) => add('error', m) }),
    [add]
  );

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>{t.message}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);

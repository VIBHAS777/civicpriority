import { createContext, useContext, useCallback, useState } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const show = useCallback((message, type = 'success') => {
    setToast({ message, type, key: Date.now() });
    setTimeout(() => setToast(null), 3200);
  }, []);
  return <ToastContext.Provider value={{ toast, show }}>{children}</ToastContext.Provider>;
}

export function useToast() { return useContext(ToastContext); }

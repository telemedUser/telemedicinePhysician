import { useCallback, useState } from 'react';
import type { ToastMessage } from '@telemedicine/design-system';

/** Minimal transient-toast hook shared by the physician pages. */
export function useToasts() {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const push = useCallback((message: string, variant: ToastMessage['variant'] = 'success') => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, variant, message }]);
    window.setTimeout(() => setToasts((prev) => prev.filter((toast) => toast.id !== id)), 3000);
  }, []);

  return { toasts, push };
}

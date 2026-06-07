import { useEffect } from 'react';
import { XCircle, X } from 'lucide-react';

interface ErrorToastProps {
  message: string;
  onDismiss: () => void;
  duration?: number;
}

export function ErrorToast({ message, onDismiss, duration = 5000 }: ErrorToastProps) {
  useEffect(() => {
    if (duration <= 0) return;
    const timer = setTimeout(onDismiss, duration);
    return () => clearTimeout(timer);
  }, [onDismiss, duration]);

  return (
    <div
      className="
        animate-slide-in
        fixed top-4 right-4 z-50 max-w-sm w-full
        glass rounded-xl shadow-lg
        border border-[var(--color-danger)]/30
        p-4
      "
      role="alert"
    >
      <div className="flex items-start gap-3">
        <XCircle size={20} className="text-[var(--color-danger)] flex-shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-text-primary">请求出错</p>
          <p className="text-xs text-text-secondary mt-1 break-words">{message}</p>
        </div>
        <button
          onClick={onDismiss}
          className="flex-shrink-0 text-text-secondary hover:text-text-primary transition-colors"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
}

import type { ReactNode } from 'react';

export function LoadingSpinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div role="status" aria-label={label} style={{ display: 'inline-block' }}>
      <span
        style={{
          display: 'inline-block',
          width: 24,
          height: 24,
          border: '3px solid var(--tm-color-border)',
          borderTopColor: 'var(--tm-color-primary)',
          borderRadius: '50%',
          animation: 'tm-spin 0.8s linear infinite',
        }}
      />
      <style>{`@keyframes tm-spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

export function ErrorMessage({ title, message }: { title?: string; message: string }) {
  return (
    <div role="alert" style={{ padding: 16, borderRadius: 8, background: '#fee2e2', color: '#991b1b' }}>
      {title && <strong>{title}</strong>}
      <p style={{ margin: title ? '8px 0 0' : 0 }}>{message}</p>
    </div>
  );
}

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div style={{ textAlign: 'center', padding: 48, color: 'var(--tm-color-text-muted)' }}>
      <h3 style={{ margin: '0 0 8px' }}>{title}</h3>
      {description && <p style={{ margin: 0 }}>{description}</p>}
    </div>
  );
}

export function Skeleton({ width = '100%', height = 16 }: { width?: string | number; height?: number }) {
  return (
    <div
      aria-hidden="true"
      style={{
        width,
        height,
        borderRadius: 4,
        background: 'linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%)',
        backgroundSize: '200% 100%',
        animation: 'tm-shimmer 1.5s infinite',
      }}
    />
  );
}

export interface ToastMessage {
  id: string;
  variant: 'success' | 'error' | 'warning' | 'info';
  message: string;
}

export function ToastContainer({ toasts }: { toasts: ToastMessage[] }) {
  return (
    <div
      role="region"
      aria-label="Notifications"
      style={{ position: 'fixed', bottom: 24, right: 24, display: 'flex', flexDirection: 'column', gap: 8, zIndex: 1100 }}
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="status"
          style={{
            padding: '12px 16px',
            borderRadius: 8,
            background: '#fff',
            boxShadow: 'var(--tm-shadow-md)',
            borderLeft: `4px solid var(--tm-color-${toast.variant === 'error' ? 'danger' : toast.variant})`,
          }}
        >
          {toast.message}
        </div>
      ))}
    </div>
  );
}

export function KpiCard({ label, value, trend }: { label: string; value: string | number; trend?: string }) {
  return (
    <div style={{ padding: 16, borderRadius: 8, border: '1px solid var(--tm-color-border)', background: 'var(--tm-color-bg)' }}>
      <div style={{ fontSize: '0.75rem', color: 'var(--tm-color-text-muted)' }}>{label}</div>
      <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: 4 }}>{value}</div>
      {trend && <div style={{ fontSize: '0.75rem', marginTop: 4 }}>{trend}</div>}
    </div>
  );
}

export function MetricBar({
  label,
  value,
  max = 100,
  unit,
}: {
  label: string;
  value: number;
  max?: number;
  unit?: string;
}) {
  const pct = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: 4 }}>
        <span>{label}</span>
        <span>
          {value}
          {unit ?? ''}
        </span>
      </div>
      <div
        role="meter"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={label}
        style={{ height: 8, background: 'var(--tm-color-border)', borderRadius: 4, overflow: 'hidden' }}
      >
        <div
          style={{
            width: `${pct}%`,
            height: '100%',
            background: 'var(--tm-color-primary)',
            borderRadius: 4,
            transition: 'width 0.3s ease',
          }}
        />
      </div>
    </div>
  );
}

export function VitalSign({ label, value, unit }: { label: string; value: string | number; unit?: string }) {
  return (
    <div style={{ textAlign: 'center', padding: 12 }}>
      <div style={{ fontSize: '0.75rem', color: 'var(--tm-color-text-muted)' }}>{label}</div>
      <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>
        {value}
        {unit && <span style={{ fontSize: '0.875rem', marginLeft: 4 }}>{unit}</span>}
      </div>
    </div>
  );
}

export function TimerDisplay({ seconds }: { seconds: number }) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return (
    <time aria-live="polite" style={{ fontVariantNumeric: 'tabular-nums', fontSize: '1.5rem' }}>
      {String(mins).padStart(2, '0')}:{String(secs).padStart(2, '0')}
    </time>
  );
}

export function ConsentBanner({ children }: { children: ReactNode }) {
  return (
    <div
      role="region"
      aria-label="Consent notice"
      style={{ padding: 16, background: 'var(--tm-color-bg-muted)', borderRadius: 8, border: '1px solid var(--tm-color-border)' }}
    >
      {children}
    </div>
  );
}

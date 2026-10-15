import type { ReactNode } from 'react';
import styles from './StatusBadge.module.css';

export type BadgeVariant = 'ok' | 'pending' | 'degraded' | 'info' | 'neutral';

export interface StatusBadgeProps {
  variant?: BadgeVariant;
  children: ReactNode;
}

export function StatusBadge({ variant = 'neutral', children }: StatusBadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[variant]}`} role="status">
      {children}
    </span>
  );
}

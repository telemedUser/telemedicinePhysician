import type { ReactNode } from 'react';

export interface ShellLayoutProps {
  title: string;
  children: ReactNode;
  nav?: ReactNode;
}

export function ShellLayout({ title, children, nav }: ShellLayoutProps) {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid var(--tm-color-border, #e5e7eb)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <h1 style={{ margin: 0, fontSize: '1.25rem' }}>{title}</h1>
        {nav}
      </header>
      <main style={{ flex: 1, padding: '24px' }}>{children}</main>
    </div>
  );
}

export function PageLayout({ children }: { children: ReactNode }) {
  return <div style={{ maxWidth: 960, margin: '0 auto' }}>{children}</div>;
}

export function Stack({ children, gap = 16 }: { children: ReactNode; gap?: number }) {
  return <div style={{ display: 'flex', flexDirection: 'column', gap }}>{children}</div>;
}

export function Inline({ children, gap = 8 }: { children: ReactNode; gap?: number }) {
  return <div style={{ display: 'flex', alignItems: 'center', gap, flexWrap: 'wrap' }}>{children}</div>;
}

export function SplitPanel({ left, right }: { left: ReactNode; right: ReactNode }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
      <div>{left}</div>
      <div>{right}</div>
    </div>
  );
}

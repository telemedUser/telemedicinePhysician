import type { ReactNode } from 'react';

export interface StepperProps {
  steps: string[];
  currentStep: number;
}

export function Stepper({ steps, currentStep }: StepperProps) {
  return (
    <nav aria-label="Progress">
      <ol style={{ display: 'flex', listStyle: 'none', padding: 0, margin: 0, gap: 8 }}>
        {steps.map((step, index) => {
          const isComplete = index < currentStep;
          const isCurrent = index === currentStep;
          return (
            <li
              key={step}
              aria-current={isCurrent ? 'step' : undefined}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: 8,
                background: isCurrent
                  ? 'var(--tm-color-primary, #0066cc)'
                  : isComplete
                    ? 'var(--tm-color-bg-muted, #f9fafb)'
                    : 'transparent',
                color: isCurrent ? '#fff' : 'var(--tm-color-text, #111)',
                fontSize: '0.875rem',
                textAlign: 'center',
              }}
            >
              {step}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

export function Tabs({
  tabs,
  active,
  onChange,
}: {
  tabs: { id: string; label: string }[];
  active: string;
  onChange: (id: string) => void;
}) {
  return (
    <div role="tablist" style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--tm-color-border)' }}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          role="tab"
          aria-selected={active === tab.id}
          type="button"
          onClick={() => onChange(tab.id)}
          style={{
            padding: '8px 16px',
            border: 'none',
            background: 'none',
            borderBottom: active === tab.id ? '2px solid var(--tm-color-primary)' : '2px solid transparent',
            cursor: 'pointer',
          }}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

export function Breadcrumbs({ items }: { items: string[] }) {
  return (
    <nav aria-label="Breadcrumb">
      <ol style={{ display: 'flex', gap: 8, listStyle: 'none', padding: 0, margin: 0, fontSize: '0.875rem' }}>
        {items.map((item, i) => (
          <li key={item}>
            {i > 0 && <span aria-hidden="true"> / </span>}
            {item}
          </li>
        ))}
      </ol>
    </nav>
  );
}

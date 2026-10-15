import type { CSSProperties, ReactNode } from 'react';

/** Material Symbols Rounded icon. */
export function Icon({
  name,
  size = 20,
  color,
  fill = false,
  style,
}: {
  name: string;
  size?: number;
  color?: string;
  fill?: boolean;
  style?: CSSProperties;
}) {
  return (
    <span
      className="material-symbols-rounded"
      aria-hidden="true"
      style={{
        fontSize: size,
        color,
        fontVariationSettings: fill ? "'FILL' 1" : "'FILL' 0",
        ...style,
      }}
    >
      {name}
    </span>
  );
}

/** Initials avatar in a tinted circle (prototype pattern). */
export function Avatar({
  initials,
  size = 36,
  tone = 'blue',
}: {
  initials: string;
  size?: number;
  tone?: 'blue' | 'navy' | 'green' | 'amber';
}) {
  const tones: Record<string, [string, string]> = {
    blue: ['var(--bp-blue-tint)', 'var(--bp-blue-dark)'],
    navy: ['#dbe6f3', 'var(--bp-navy)'],
    green: ['var(--bp-ok-bg)', 'var(--bp-ok)'],
    amber: ['var(--bp-warn-bg)', 'var(--bp-warn)'],
  };
  const [bg, fg] = tones[tone] ?? tones.blue;
  return (
    <span
      aria-hidden="true"
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: bg,
        color: fg,
        display: 'grid',
        placeItems: 'center',
        fontWeight: 700,
        fontSize: size * 0.36,
        flex: 'none',
      }}
    >
      {initials}
    </span>
  );
}

export type Tone = 'neutral' | 'blue' | 'green' | 'amber' | 'red' | 'navy';

const toneStyles: Record<Tone, CSSProperties> = {
  neutral: { background: '#eef0f2', color: 'var(--bp-muted)' },
  blue: { background: 'var(--bp-info-bg)', color: 'var(--bp-info)' },
  green: { background: 'var(--bp-ok-bg)', color: 'var(--bp-ok)' },
  amber: { background: 'var(--bp-warn-bg)', color: 'var(--bp-warn)' },
  red: { background: 'var(--bp-danger-bg)', color: 'var(--bp-danger)' },
  navy: { background: '#dbe6f3', color: 'var(--bp-navy)' },
};

/** Pill badge. */
export function Pill({
  children,
  tone = 'neutral',
  dot = false,
  icon,
}: {
  children: ReactNode;
  tone?: Tone;
  dot?: boolean;
  icon?: string;
}) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        padding: '3px 10px',
        borderRadius: 'var(--bp-pill)',
        fontSize: 12,
        fontWeight: 600,
        lineHeight: 1.4,
        whiteSpace: 'nowrap',
        ...toneStyles[tone],
      }}
    >
      {dot && (
        <span
          style={{
            width: 7,
            height: 7,
            borderRadius: '50%',
            background: 'currentColor',
            flex: 'none',
          }}
        />
      )}
      {icon && <Icon name={icon} size={14} />}
      {children}
    </span>
  );
}

/** Card / panel surface. */
export function Card({
  children,
  style,
  padded = true,
  interactive = false,
  as: Tag = 'div',
  ...rest
}: {
  children: ReactNode;
  style?: CSSProperties;
  padded?: boolean;
  interactive?: boolean;
  as?: 'div' | 'section' | 'article';
} & React.HTMLAttributes<HTMLElement>) {
  return (
    <Tag
      style={{
        background: 'var(--bp-surface)',
        border: '1px solid var(--bp-border-2)',
        borderRadius: 'var(--bp-radius)',
        boxShadow: 'var(--bp-shadow)',
        padding: padded ? 18 : 0,
        transition: 'box-shadow .15s ease, transform .15s ease',
        ...(interactive ? { cursor: 'pointer' } : {}),
        ...style,
      }}
      {...rest}
    >
      {children}
    </Tag>
  );
}

/** Branded button. */
export function BpButton({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  loading = false,
  disabled,
  type = 'button',
  onClick,
  style,
}: {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'subtle';
  size?: 'sm' | 'md';
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
  type?: 'button' | 'submit';
  onClick?: () => void;
  style?: CSSProperties;
}) {
  const base: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 7,
    borderRadius: 'var(--bp-radius-sm)',
    fontWeight: 600,
    fontSize: size === 'sm' ? 13 : 14,
    padding: size === 'sm' ? '6px 12px' : '9px 16px',
    border: '1px solid transparent',
    cursor: disabled || loading ? 'default' : 'pointer',
    opacity: disabled ? 0.55 : 1,
    whiteSpace: 'nowrap',
    transition: 'background .15s ease, border-color .15s ease',
  };
  const variants: Record<string, CSSProperties> = {
    primary: { background: 'var(--bp-blue)', color: '#fff' },
    secondary: {
      background: 'var(--bp-surface)',
      color: 'var(--bp-blue-dark)',
      borderColor: 'var(--bp-border)',
    },
    ghost: { background: 'transparent', color: 'var(--bp-muted)' },
    subtle: { background: 'var(--bp-blue-tint)', color: 'var(--bp-blue-dark)' },
    danger: { background: 'var(--bp-danger)', color: '#fff' },
  };
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      aria-busy={loading}
      style={{ ...base, ...variants[variant], ...style }}
    >
      {loading ? (
        <span
          style={{
            width: 14,
            height: 14,
            border: '2px solid currentColor',
            borderTopColor: 'transparent',
            borderRadius: '50%',
            animation: 'bpSpin .7s linear infinite',
          }}
        />
      ) : (
        icon && <Icon name={icon} size={size === 'sm' ? 16 : 18} />
      )}
      {children}
    </button>
  );
}

/** Section header with optional subtitle and right-aligned actions. */
export function SectionHeader({
  title,
  subtitle,
  actions,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16, flexWrap: 'wrap' }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>{title}</h2>
        {subtitle && (
          <p style={{ margin: '4px 0 0', color: 'var(--bp-muted)', fontSize: 14 }}>{subtitle}</p>
        )}
      </div>
      {actions && <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>{actions}</div>}
    </div>
  );
}

/** Uppercase label used above field groups / meta rows. */
export function Overline({ children }: { children: ReactNode }) {
  return (
    <span
      style={{
        fontSize: 11,
        fontWeight: 600,
        color: 'var(--bp-muted)',
        textTransform: 'uppercase',
        letterSpacing: '.06em',
      }}
    >
      {children}
    </span>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label style={{ display: 'block' }}>
      <span
        style={{
          display: 'block',
          marginBottom: 6,
          fontSize: 13,
          fontWeight: 600,
          color: 'var(--bp-text-2)',
        }}
      >
        {label}
      </span>
      {children}
    </label>
  );
}

const controlStyle: CSSProperties = {
  width: '100%',
  padding: '9px 12px',
  borderRadius: 'var(--bp-radius-sm)',
  border: '1px solid var(--bp-border)',
  fontSize: 14,
  color: 'var(--bp-text)',
  background: 'var(--bp-surface)',
  outline: 'none',
};

export function BpInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} style={{ ...controlStyle, ...props.style }} />;
}
export function BpTextArea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} style={{ ...controlStyle, minHeight: 90, resize: 'vertical', ...props.style }} />;
}
export function BpSelect(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} style={{ ...controlStyle, cursor: 'pointer', ...props.style }} />;
}

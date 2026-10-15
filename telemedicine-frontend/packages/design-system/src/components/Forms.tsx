import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';

export interface FormFieldProps {
  label: string;
  hint?: string;
  error?: string;
  children: ReactNode;
  htmlFor?: string;
}

export function FormField({ label, hint, error, children, htmlFor }: FormFieldProps) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label htmlFor={htmlFor} style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
        {label}
      </label>
      {children}
      {hint && !error && (
        <p id={`${htmlFor}-hint`} style={{ margin: '4px 0 0', fontSize: '0.875rem', color: 'var(--tm-color-text-muted)' }}>
          {hint}
        </p>
      )}
      {error && (
        <p id={`${htmlFor}-error`} role="alert" style={{ margin: '4px 0 0', fontSize: '0.875rem', color: 'var(--tm-color-danger)' }}>
          {error}
        </p>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 12px',
  borderRadius: 8,
  border: '1px solid var(--tm-color-border)',
  fontSize: '1rem',
};

export function TextField(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} style={{ ...inputStyle, ...props.style }} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} style={{ ...inputStyle, minHeight: 80, ...props.style }} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} style={{ ...inputStyle, ...props.style }} />;
}

export function Checkbox({ label, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <input type="checkbox" {...props} />
      {label}
    </label>
  );
}

export function SearchInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input type="search" placeholder="Search…" {...props} style={{ ...inputStyle, ...props.style }} />;
}

export interface DataTableProps<T> {
  columns: { key: keyof T; header: string }[];
  data: T[];
  onSort?: (key: keyof T) => void;
}

export function DataTable<T extends Record<string, unknown>>({ columns, data, onSort }: DataTableProps<T>) {
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <thead>
        <tr>
          {columns.map((col) => (
            <th
              key={String(col.key)}
              scope="col"
              style={{ textAlign: 'left', padding: 8, borderBottom: '2px solid var(--tm-color-border)' }}
            >
              {onSort ? (
                <button type="button" onClick={() => onSort(col.key)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                  {col.header}
                </button>
              ) : (
                col.header
              )}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row, i) => (
          <tr key={i}>
            {columns.map((col) => (
              <td key={String(col.key)} style={{ padding: 8, borderBottom: '1px solid var(--tm-color-border)' }}>
                {String(row[col.key] ?? '')}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

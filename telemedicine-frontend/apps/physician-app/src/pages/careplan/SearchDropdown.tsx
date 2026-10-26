import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Icon } from '../../ui/primitives';

export interface SearchDropdownProps<T> {
  placeholder: string;
  /** Debounced search against the DB-backed directory. */
  search: (q: string) => Promise<T[]>;
  /** Primary label for an option (e.g. the title / drug name). */
  renderLabel: (item: T) => string;
  /** Short code/badge shown on the left (e.g. ICD-10 code). Optional. */
  renderBadge?: (item: T) => string | undefined;
  /** Muted sub-line (e.g. strength · form). Optional. */
  renderSub?: (item: T) => string | undefined;
  onSelect: (item: T) => void;
  autoFocus?: boolean;
}

/**
 * Generic type-ahead backed by a directory search endpoint. The options list renders in a PORTAL
 * with fixed positioning anchored to the input, so no ancestor `overflow:hidden` card can clip it.
 * Opens on focus (blank query returns a default slice), debounces input, and calls onSelect.
 */
export function SearchDropdown<T>({
  placeholder,
  search,
  renderLabel,
  renderBadge,
  renderSub,
  onSelect,
  autoFocus,
}: SearchDropdownProps<T>) {
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<T[]>([]);
  const [rect, setRect] = useState<{ left: number; top: number; width: number } | null>(null);
  const anchorRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Debounced search while open.
  useEffect(() => {
    if (!open) return;
    let active = true;
    const t = window.setTimeout(() => {
      void search(q).then((rows) => {
        if (active) setItems(rows);
      });
    }, 180);
    return () => {
      active = false;
      window.clearTimeout(t);
    };
  }, [q, open, search]);

  // Position the portal under the input; keep it aligned on scroll/resize.
  const place = () => {
    const el = anchorRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    setRect({ left: r.left, top: r.bottom + 4, width: r.width });
  };
  useLayoutEffect(() => {
    if (open) place();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);
  useEffect(() => {
    if (!open) return;
    const onMove = () => place();
    window.addEventListener('scroll', onMove, true);
    window.addEventListener('resize', onMove);
    return () => {
      window.removeEventListener('scroll', onMove, true);
      window.removeEventListener('resize', onMove);
    };
  }, [open]);

  // Close on outside click (anchor or menu).
  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      const t = e.target as Node;
      if (anchorRef.current?.contains(t) || menuRef.current?.contains(t)) return;
      setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  return (
    <div ref={anchorRef} style={{ position: 'relative' }}>
      <div className="cp-search">
        <Icon name="search" size={18} color="var(--bp-muted)" />
        <input
          autoFocus={autoFocus}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onFocus={() => setOpen(true)}
          placeholder={placeholder}
          style={{ flex: 1, border: 'none', outline: 'none', font: '15px Figtree, system-ui, sans-serif', background: 'transparent' }}
        />
      </div>
      {open && items.length > 0 && rect &&
        createPortal(
          <div
            ref={menuRef}
            className="cp-menu"
            style={{ position: 'fixed', left: rect.left, top: rect.top, width: rect.width, zIndex: 3000 }}
          >
            {items.map((item, i) => {
              const badge = renderBadge?.(item);
              const sub = renderSub?.(item);
              return (
                <button
                  key={i}
                  type="button"
                  className="cp-menu-item"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    onSelect(item);
                    setQ('');
                    setOpen(false);
                  }}
                >
                  {badge && <span className="cp-menu-badge">{badge}</span>}
                  <span style={{ flex: 1, minWidth: 0 }}>
                    <span style={{ display: 'block', fontSize: 14, color: 'var(--bp-text)' }}>{renderLabel(item)}</span>
                    {sub && <span style={{ display: 'block', fontSize: 12, color: 'var(--bp-muted)' }}>{sub}</span>}
                  </span>
                  <Icon name="add" size={18} color="var(--bp-blue)" />
                </button>
              );
            })}
          </div>,
          document.body,
        )}
    </div>
  );
}

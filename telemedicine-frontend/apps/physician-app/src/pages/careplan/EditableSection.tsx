import type { ReactNode } from 'react';
import { Icon } from '../../ui/primitives';

export interface EditableSectionProps {
  icon: string;
  title: string;
  editing: boolean;
  onToggle: () => void;
  /** Optional right-side accessory before the edit control (e.g. a toggle switch). */
  accessory?: ReactNode;
  hideToggle?: boolean;
  children: ReactNode;
}

/**
 * A white care-plan section card matching the D-08 prototype: blue icon chip + title on the left,
 * an edit pencil that becomes a blue "Done" pill while editing. The child renders read vs edit view
 * based on `editing`.
 */
export function EditableSection({
  icon,
  title,
  editing,
  onToggle,
  accessory,
  hideToggle,
  children,
}: EditableSectionProps) {
  return (
    <div className="cp-card">
      <div className="cp-head" style={{ borderBottom: '1px solid var(--bp-border-2)' }}>
        <span className="cp-head-icon">
          <Icon name={icon} size={18} />
        </span>
        <span className="cp-head-title" style={{ flex: 1 }}>
          {title}
        </span>
        {accessory}
        {!hideToggle &&
          (editing ? (
            <button type="button" className="cp-approve" onClick={onToggle}>
              <Icon name="check" size={16} color="#fff" />
              Done
            </button>
          ) : (
            <button
              type="button"
              className="cp-pencil"
              onClick={onToggle}
              aria-label={`Edit ${title}`}
              title={`Edit ${title}`}
            >
              <Icon name="edit" size={18} />
            </button>
          ))}
      </div>
      <div style={{ padding: 18 }}>{children}</div>
    </div>
  );
}

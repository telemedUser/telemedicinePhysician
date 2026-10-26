import { Icon } from '../../ui/primitives';

/**
 * The Review → Send → Close step pills shared by D-08 (Review) and D-09 (Send).
 * `current` is the active step index (0-based). Steps before it render as completed (green check).
 */
export function CarePlanStepper({ current }: { current: number }) {
  const steps = ['Review', 'Send', 'Close'];
  return (
    <div className="cp-steps">
      {steps.map((s, i) => {
        const complete = i < current;
        const active = i === current;
        const cls = active ? 'cp-step is-active' : complete ? 'cp-step is-complete' : 'cp-step';
        return (
          <span key={s} style={{ display: 'inline-flex', alignItems: 'center' }}>
            <span className={cls}>
              <span className="cp-step-num">
                {complete ? <Icon name="check" size={14} color="#fff" /> : i + 1}
              </span>
              {s}
            </span>
            {i < steps.length - 1 && <span className="cp-step-sep" />}
          </span>
        );
      })}
    </div>
  );
}

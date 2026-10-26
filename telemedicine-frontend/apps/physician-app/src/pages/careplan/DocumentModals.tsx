import { useEffect, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import type { CarePlan } from '../../api/types';
import { Icon } from '../../ui/primitives';

/**
 * The read-only document overlays opened from D-10 "View" (Prescription, Medical certificate).
 * Every clinical value flows from the saved care plan (D-08) + the patient/physician context; only
 * issuance metadata (document number, signing timestamp) is stamped at close time.
 */

export interface DocParties {
  patientName: string;
  patientSub: string;
  physicianName: string;
  physicianSub: string;
}

export interface DocMeta {
  /** Downstream system label (routing destination). */
  system: string;
  systemSub: string;
  /** Document number, e.g. Rx 77412-01 / ML-5567021. */
  number: string;
  /** Issue/signature timestamp, formatted. */
  issuedAt: string;
}

const DATE_TIME: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' };
export function fmtDate(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('en-GB', DATE_TIME);
}
export function fmtDateTime(value?: string | Date | null): string {
  if (!value) return '';
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const date = d.toLocaleDateString('en-GB', DATE_TIME);
  const time = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  return `${date} · ${time}`;
}

/** Modal shell: dim backdrop, white sheet, title + close, Esc to dismiss. */
function DocShell({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  return createPortal(
    <div className="doc-overlay" role="dialog" aria-modal="true" aria-label={title} onMouseDown={onClose}>
      <div className="doc-modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="doc-modal-head">
          <h2>{title}</h2>
          <button type="button" className="doc-modal-close" aria-label="Close" onClick={onClose}>
            <Icon name="close" size={22} />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  );
}

function Parties({ parties }: { parties: DocParties }) {
  return (
    <div className="doc-parties">
      <div>
        <div className="doc-party-label">Patient</div>
        <div className="doc-party-name" data-phi="true">{parties.patientName}</div>
        <div className="doc-party-sub" data-phi="true">{parties.patientSub}</div>
      </div>
      <div>
        <div className="doc-party-label">Physician</div>
        <div className="doc-party-name">{parties.physicianName}</div>
        <div className="doc-party-sub">{parties.physicianSub}</div>
      </div>
    </div>
  );
}

function Issuer({ meta }: { meta: DocMeta }) {
  return (
    <div className="doc-issuer">
      <div>
        <div className="doc-issuer-sys">{meta.system}</div>
        <div className="doc-issuer-sub">{meta.systemSub}</div>
      </div>
      <div className="doc-issuer-meta">
        <div>{meta.number}</div>
        <div>{meta.issuedAt}</div>
      </div>
    </div>
  );
}

function Signature({ parties, meta, badge }: { parties: DocParties; meta: DocMeta; badge: string }) {
  return (
    <div className="doc-sign">
      <div>
        <div className="doc-sign-name">{parties.physicianName}</div>
        <div className="doc-sign-sub">{parties.physicianSub} · {meta.issuedAt}</div>
      </div>
      <span className="doc-sign-badge"><Icon name="verified" size={14} />{badge}</span>
    </div>
  );
}

/** Prescription document. */
export function PrescriptionModal({
  plan,
  parties,
  meta,
  onClose,
}: {
  plan: CarePlan;
  parties: DocParties;
  meta: DocMeta;
  onClose: () => void;
}) {
  const primary = plan.diagnoses.find((d) => d.primary) ?? plan.diagnoses[0];
  const allergyNote = plan.note.assessment?.toLowerCase().includes('allerg') ? plan.note.assessment : undefined;
  return (
    <DocShell title="Prescription" onClose={onClose}>
      <div className="doc-sheet">
        <div className="doc-sheet-inner">
          <Issuer meta={meta} />
          <div className="doc-doctype">Prescription</div>
          <Parties parties={parties} />

          <div className="doc-sec-label">Medicines</div>
          {plan.prescriptions.length > 0 ? (
            plan.prescriptions.map((m, i) => {
              const name = `${m.name}${m.strength ? ` · ${m.strength}` : ''}${m.form ? ` ${m.form}` : ''}`;
              const dir = [
                m.dosage,
                m.frequency,
                m.durationDays ? `${m.durationDays} days` : null,
                m.quantity ? `Qty ${m.quantity}` : null,
                m.instructions,
              ]
                .filter(Boolean)
                .join(' · ');
              return (
                <div className="doc-med" key={i}>
                  <div className="doc-med-name">{name}</div>
                  <div className="doc-med-dir">{dir || '—'}</div>
                </div>
              );
            })
          ) : (
            <div className="doc-sec-body">No medicines prescribed.</div>
          )}

          <div className="doc-sec-label">Diagnosis</div>
          <div className="doc-sec-body">{primary ? `${primary.code} ${primary.title}` : '—'}</div>

          {allergyNote && (
            <>
              <div className="doc-sec-label">Allergies</div>
              <div className="doc-sec-body">{allergyNote}</div>
            </>
          )}

          <Signature parties={parties} meta={meta} badge="Electronic signature · valid at any pharmacy" />
        </div>
      </div>
    </DocShell>
  );
}

/** Medical certificate (sick-leave) document. */
export function MedicalCertificateModal({
  plan,
  parties,
  meta,
  durationDays,
  onClose,
}: {
  plan: CarePlan;
  parties: DocParties;
  meta: DocMeta;
  durationDays: number;
  onClose: () => void;
}) {
  const ml = plan.medicalLeave;
  const primary = plan.diagnoses.find((d) => d.primary) ?? plan.diagnoses[0];
  return (
    <DocShell title="Medical certificate" onClose={onClose}>
      <div className="doc-sheet">
        <div className="doc-sheet-inner">
          <Issuer meta={meta} />
          <div className="doc-doctype">Medical certificate</div>
          <Parties parties={parties} />

          <div className="doc-sec-label">Leave</div>
          <div className="doc-kv"><span className="doc-kv-label">Type</span><span className="doc-kv-value">{ml.reason?.trim() || '—'}</span></div>
          <div className="doc-kv"><span className="doc-kv-label">From</span><span className="doc-kv-value">{fmtDate(ml.startDate) || '—'}</span></div>
          <div className="doc-kv"><span className="doc-kv-label">To</span><span className="doc-kv-value">{fmtDate(ml.endDate) || '—'}</span></div>
          <div className="doc-kv"><span className="doc-kv-label">Duration</span><span className="doc-kv-value">{durationDays} day{durationDays === 1 ? '' : 's'}</span></div>

          <div className="doc-sec-label">Diagnosis</div>
          <div className="doc-sec-body">{primary ? `${primary.code} ${primary.title}` : '—'}</div>

          <div className="doc-sec-label">Note</div>
          <div className="doc-sec-body">{plan.summary.instructions?.trim() || 'Rest at home; fit to return at the end of the leave unless reviewed.'}</div>

          <Signature parties={parties} meta={meta} badge="Electronic signature" />
        </div>
      </div>
    </DocShell>
  );
}

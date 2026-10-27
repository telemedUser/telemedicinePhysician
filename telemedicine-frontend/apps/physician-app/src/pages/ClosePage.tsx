import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type { CarePlan, PatientDetail, ProviderDetail } from '../api/types';
import { BpButton, Card, Icon } from '../ui/primitives';
import { CarePlanStepper } from './careplan/CarePlanStepper';
import {
  MedicalCertificateModal,
  PrescriptionModal,
  fmtDateTime,
  type DocMeta,
  type DocParties,
} from './careplan/DocumentModals';
import { getPhysicianSession } from '../auth/physicianSession';
import { CURRENT_PHYSICIAN } from '../data/mockData';

/**
 * D-10 Close consultation. Shows the minimum-set status and a read-only list of what will be sent
 * when the consultation closes; Prescription and Medical-leave rows open a document overlay. Every
 * value is derived from the saved care plan (D-08) carried through D-09 — nothing is authored here.
 */

/** Routing destinations per document (neutral labels, not third-party vendor names). */
const DEST = {
  prescription: 'Pharmacy network',
  leave: 'Leave registry',
} as const;

function inclusiveDays(start?: string, end?: string): number {
  if (!start || !end) return 0;
  const s = new Date(start);
  const e = new Date(end);
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return 0;
  return Math.max(1, Math.round((e.getTime() - s.getTime()) / 86400000) + 1);
}

/** HH:MM clock label. */
function fmtClock(d: Date): string {
  return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

/** Deterministic document number from the appointment id, so it is stable (not random per render). */
function docNumber(prefix: string, appointmentId: string, salt: number): string {
  let h = salt;
  for (const ch of appointmentId) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  const n = (h % 9_000_000) + 1_000_000;
  return `${prefix}-${n}`;
}

export function ClosePage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const api = usePhysicianApi();

  const [plan, setPlan] = useState<CarePlan | null>(null);
  const [patient, setPatient] = useState<PatientDetail | null>(null);
  const [provider, setProvider] = useState<ProviderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [openDoc, setOpenDoc] = useState<'prescription' | 'leave' | null>(null);
  const [showException, setShowException] = useState(false);
  const [reason, setReason] = useState('');
  const [closed, setClosed] = useState(false);
  const [closedAt, setClosedAt] = useState<Date | null>(null);
  const [delivered, setDelivered] = useState(false);
  const [deliveredAt, setDeliveredAt] = useState<Date | null>(null);
  const [toasts, setToasts] = useState<{ id: string; title: string; body: string }[]>([]);

  const session = getPhysicianSession();

  const pushToast = (title: string, body: string) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, title, body }]);
    window.setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 8000);
  };

  // After close, poll the patient-side documents projection (via the physician host's /booking-api
  // proxy) until the ConsultationDocumentsIssued event has been received and projected — then flip
  // the delivery status from "Sending…" to "Delivered" and raise the "Documents delivered" toast.
  useEffect(() => {
    if (!closed || delivered) return;
    let active = true;
    const poll = () => {
      void fetch(`/booking-api/consultations/${encodeURIComponent(appointmentId)}/documents`)
        .then((r) => (r.ok ? r.json() : null))
        .then((d: { status?: string } | null) => {
          if (active && d && d.status === 'ISSUED') {
            const at = new Date();
            setDelivered(true);
            setDeliveredAt(at);
            pushToast('Documents delivered · ' + fmtClock(at), "Prescription, medical leave and summary are in the patient's app.");
          }
        })
        .catch(() => undefined);
    };
    poll();
    const timer = window.setInterval(poll, 3000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [closed, delivered, appointmentId]);

  useEffect(() => {
    let active = true;
    const providerId = session?.physicianId ?? CURRENT_PHYSICIAN.id;
    void Promise.all([
      api.getCarePlan(appointmentId),
      api.getPatientDetail(appointmentId),
      api.getProvider(providerId),
    ]).then(([p, d, pr]) => {
      if (!active) return;
      setPlan(p);
      setPatient(d);
      setProvider(pr);
      setLoading(false);
    });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api, appointmentId]);

  // Minimum set = clinical note present + at least one diagnosis (mirrors backend close rule).
  const notePresent = Boolean(
    plan && (plan.note.history || plan.note.findings || plan.note.assessment || plan.note.plan),
  );
  const hasDiagnosis = Boolean(plan && plan.diagnoses.length > 0);
  const minimumSatisfied = notePresent && hasDiagnosis;

  const issuedAt = useMemo(() => fmtDateTime(new Date()), []);

  const parties: DocParties = useMemo(() => {
    const physicianName = session?.displayName ?? CURRENT_PHYSICIAN.name;
    const spec = provider?.specialties?.[0] ?? CURRENT_PHYSICIAN.specialty;
    const reg = provider?.credentials?.find((c) => /registration/i.test(c.type))?.number;
    const age = patient?.ageYears ? `${patient.ageYears} y` : '';
    const scheme = patient?.insuranceScheme ?? '';
    const id = patient?.nationalIdMasked ?? '';
    return {
      patientName: patient?.patientDisplay ?? appointmentId,
      patientSub: [id, age, scheme].filter(Boolean).join(' · '),
      physicianName,
      physicianSub: [spec, CURRENT_PHYSICIAN.clinic, reg ? `Reg. ${reg}` : null].filter(Boolean).join(' · '),
    };
  }, [patient, provider, session, appointmentId]);

  const rxMeta: DocMeta = {
    system: 'Connected Care · e-Prescription',
    systemSub: 'Electronic prescription',
    number: docNumber('Rx', appointmentId, 7),
    issuedAt,
  };
  const mlMeta: DocMeta = {
    system: 'Connected Care · Medical leave',
    systemSub: 'Electronic medical certificate',
    number: docNumber('ML', appointmentId, 13),
    issuedAt,
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <CarePlanStepper current={2} />
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>Loading…</div>
      </div>
    );
  }

  const p = plan;
  const leaveDays = inclusiveDays(p?.medicalLeave.startDate, p?.medicalLeave.endDate);
  const medNames = (p?.prescriptions ?? []).map((m) => m.name).filter(Boolean).join(', ');
  const primaryDx = p?.diagnoses.find((d) => d.primary) ?? p?.diagnoses[0];
  const fu = p?.followUp;

  // The six "what will be sent" items, derived from the plan (shown only when they carry content).
  const items: { key: string; label: string; onView?: () => void }[] = [];
  items.push({ key: 'summary', label: 'Consultation summary' });
  if (primaryDx) items.push({ key: 'dx', label: `Diagnosis ${primaryDx.code}` });
  if ((p?.prescriptions.length ?? 0) > 0) {
    items.push({ key: 'rx', label: `Prescription${medNames ? ` (${medNames})` : ''}`, onView: () => setOpenDoc('prescription') });
  }
  if (fu?.enabled) {
    items.push({
      key: 'fu',
      label: `Follow-up · ${fu.mode?.trim() || 'video check-in'}${fu.inDays ? ` in ${fu.inDays} days` : ''}`,
    });
  }
  if (p?.medicalLeave.enabled) {
    items.push({
      key: 'ml',
      label: `Medical leave · ${leaveDays} day${leaveDays === 1 ? '' : 's'} · ${mlMeta.number}`,
      onView: () => setOpenDoc('leave'),
    });
  }
  items.push({ key: 'instructions', label: 'Patient instructions' });

  const headerMeta = [patient?.modality ?? undefined, patient?.scheduledAt ? new Date(patient.scheduledAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }) : undefined]
    .filter(Boolean)
    .join(' · ');

  // ===== Closed: delivery-status view (post-close) =====
  if (closed) {
    const statusLabel = delivered
      ? `Delivered ${deliveredAt ? fmtClock(deliveredAt) : ''}`
      : 'Sending…';
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <ToastStack toasts={toasts} onClose={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))} />

        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ flex: 'none', width: 44, height: 44, borderRadius: '50%', background: 'var(--bp-ok-bg)', display: 'grid', placeItems: 'center' }}>
            <Icon name="check_circle" size={24} color="var(--bp-ok)" fill />
          </span>
          <div>
            <h1 className="cp-title" style={{ fontSize: 24 }}>Consultation closed</h1>
            <p style={{ margin: '2px 0 0', color: 'var(--bp-muted)', fontSize: 14 }}>
              Closed {closedAt ? fmtClock(closedAt) : ''}
              {headerMeta ? ` · ${headerMeta}` : ''}
            </p>
          </div>
        </div>

        <div className="cl-card">
          <div className="cl-card-head">Delivery status</div>
          {items.map((it) => (
            <div className="cl-item" key={it.key}>
              <span className="cl-item-label">{it.label}</span>
              <span className={delivered ? 'cl-delivery is-delivered' : 'cl-delivery is-sending'}>
                <Icon name={delivered ? 'check_circle' : 'send'} size={15} />
                {statusLabel}
              </span>
              <span className="cl-view-col">
                {it.onView && <button type="button" className="cl-view" onClick={it.onView}>View</button>}
              </span>
            </div>
          ))}
        </div>

        <div>
          <Link to="/worklist">
            <BpButton icon="arrow_forward">Back to worklist</BpButton>
          </Link>
        </div>

        {openDoc === 'prescription' && p && (
          <PrescriptionModal plan={p} parties={parties} meta={rxMeta} onClose={() => setOpenDoc(null)} />
        )}
        {openDoc === 'leave' && p && (
          <MedicalCertificateModal plan={p} parties={parties} meta={mlMeta} durationDays={leaveDays} onClose={() => setOpenDoc(null)} />
        )}
      </div>
    );
  }

  const doClose = (exception: boolean) => {
    void api
      .closeConsultation(appointmentId, {
        completedDocuments: [
          ...(notePresent ? (['NOTE'] as const) : []),
          ...(hasDiagnosis ? (['DIAGNOSIS'] as const) : []),
          ...((p?.prescriptions.length ?? 0) > 0 ? (['PRESCRIPTION'] as const) : []),
          ...(fu?.enabled ? (['FOLLOW_UP'] as const) : []),
          ...(p?.medicalLeave.enabled ? (['CERTIFICATE'] as const) : []),
        ],
        exceptionReason: exception ? reason : undefined,
      })
      .finally(() => {
        const at = new Date();
        setClosedAt(at);
        setClosed(true);
        pushToast('ConsultationClosed recorded · ' + fmtClock(at), 'Consultation ' + appointmentId + ' · patient notified');
      });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 14 }}>
          <strong style={{ color: 'var(--bp-navy)' }} data-phi="true">{patient?.patientDisplay ?? appointmentId}</strong>
          {headerMeta && <span style={{ color: 'var(--bp-muted)' }}> · {headerMeta}</span>}
        </span>
        <span style={{ flex: 1 }} />
        <CarePlanStepper current={2} />
      </div>

      <h1 className="cp-title">Close consultation</h1>

      <div className={minimumSatisfied ? 'cl-banner is-ok' : 'cl-banner is-warn'}>
        <Icon name={minimumSatisfied ? 'check_circle' : 'warning'} size={20} color={minimumSatisfied ? 'var(--bp-ok)' : 'var(--bp-warn)'} />
        {minimumSatisfied ? (
          <span><strong>Minimum set satisfied</strong><span className="cl-banner-rest"> — consultation note and diagnosis are in.</span></span>
        ) : (
          <span><strong>Minimum set not met</strong><span className="cl-banner-rest"> — a consultation note and a diagnosis are required, or close with an exception.</span></span>
        )}
      </div>

      <div className="cl-card">
        <div className="cl-card-head">What will be sent when the consultation closes</div>
        {items.map((it) => (
          <div className="cl-item" key={it.key}>
            <span className="cl-item-label">{it.label}</span>
            {it.onView && <button type="button" className="cl-view" onClick={it.onView}>View</button>}
          </div>
        ))}
      </div>

      {showException && (
        <Card>
          <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--bp-muted)', marginBottom: 8 }}>
            Exception reason
          </label>
          <textarea
            className="cp-edit-field"
            rows={2}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Why the minimum set is not met…"
          />
        </Card>
      )}

      <div className="cl-actions">
        {minimumSatisfied && !showException ? (
          <button type="button" className="cl-close" onClick={() => doClose(false)}>
            <Icon name="task_alt" size={18} color="#fff" />Close consultation
          </button>
        ) : showException ? (
          <button type="button" className="cl-close" disabled={!reason.trim()} onClick={() => doClose(true)}>
            <Icon name="task_alt" size={18} color="#fff" />Close with exception
          </button>
        ) : null}
        {!showException ? (
          <button type="button" className="cl-exception" onClick={() => setShowException(true)}>Close with exception</button>
        ) : (
          <button type="button" className="cl-exception" onClick={() => { setShowException(false); setReason(''); }}>Cancel</button>
        )}
      </div>

      {openDoc === 'prescription' && p && (
        <PrescriptionModal plan={p} parties={parties} meta={rxMeta} onClose={() => setOpenDoc(null)} />
      )}
      {openDoc === 'leave' && p && (
        <MedicalCertificateModal plan={p} parties={parties} meta={mlMeta} durationDays={leaveDays} onClose={() => setOpenDoc(null)} />
      )}
    </div>
  );
}

/** Top-right transient notifications (dark cards) shown after close, matching the D-10 prototype. */
function ToastStack({
  toasts,
  onClose,
}: {
  toasts: { id: string; title: string; body: string }[];
  onClose: (id: string) => void;
}) {
  if (toasts.length === 0) return null;
  return (
    <div className="cl-toasts">
      {toasts.map((t) => (
        <div className="cl-toast" key={t.id}>
          <Icon name="notifications" size={18} color="#9fb4cc" />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="cl-toast-title">{t.title}</div>
            <div className="cl-toast-body">{t.body}</div>
          </div>
          <button type="button" className="cl-toast-close" aria-label="Dismiss" onClick={() => onClose(t.id)}>
            <Icon name="close" size={16} color="#9fb4cc" />
          </button>
        </div>
      ))}
    </div>
  );
}

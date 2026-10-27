import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { usePhysicianApi } from '../api/physicianApi';
import type {
  CarePlan,
  CarePlanPrescription,
  DiagnosisOption,
  DiagnosticTestOption,
  MedicationOption,
} from '../api/types';
import { BpButton, Field, Icon } from '../ui/primitives';
import { SearchDropdown } from './careplan/SearchDropdown';
import { CarePlanStepper } from './careplan/CarePlanStepper';

const EMPTY: CarePlan = {
  appointmentId: '',
  note: {},
  diagnoses: [],
  prescriptions: [],
  diagnosticTests: [],
  followUp: { enabled: false },
  medicalLeave: { enabled: false },
  summary: {},
  status: 'DRAFT',
};

type NoteField = 'history' | 'findings' | 'assessment' | 'plan';
type SummaryField = 'text' | 'instructions';
const NOTE_FIELDS: { key: NoteField; label: string }[] = [
  { key: 'history', label: 'History' },
  { key: 'findings', label: 'Findings' },
  { key: 'assessment', label: 'Assessment' },
  { key: 'plan', label: 'Plan' },
];

/** Follow-up "When" presets — label shown in the segmented control, value stored as inDays. */
const FOLLOW_UP_PRESETS: { num: string; unit: string; days: number }[] = [
  { num: '3', unit: 'days', days: 3 },
  { num: '5', unit: 'days', days: 5 },
  { num: '1', unit: 'week', days: 7 },
  { num: '2', unit: 'weeks', days: 14 },
  { num: '1', unit: 'month', days: 30 },
];

/** Format an ISO date (yyyy-mm-dd) or a Date as e.g. "Wed 16 Sep". Returns '' if invalid. */
function fmtShortDate(value?: string | Date | null): string {
  if (!value) return '';
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' });
}

/** Today + n days as a yyyy-mm-dd string (for computing the follow-up date from a preset). */
function todayPlus(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

/** Inclusive day count between two yyyy-mm-dd strings; falls back to 1. */
function inclusiveDays(start?: string, end?: string): number {
  if (!start || !end) return 1;
  const s = new Date(start);
  const e = new Date(end);
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return 1;
  return Math.max(1, Math.round((e.getTime() - s.getTime()) / 86400000) + 1);
}

/** end date = start + (days - 1), as yyyy-mm-dd. */
function endFrom(start: string, days: number): string {
  const d = new Date(start);
  d.setDate(d.getDate() + Math.max(1, days) - 1);
  return d.toISOString().slice(0, 10);
}

/** Medical-leave type options (DB-backed reasons could replace this later; kept as plain presets). */
const LEAVE_TYPES = [
  'Sick leave · common illness',
  'Sick leave · infectious',
  'Injury · recovery',
  'Medical procedure · recovery',
  'Mental health',
];

/** Dosing frequency options, with the number of doses per day used to auto-compute quantity. */
const FREQUENCY_OPTIONS: { label: string; perDay: number }[] = [
  { label: 'Once daily', perDay: 1 },
  { label: 'Every 12 h', perDay: 2 },
  { label: 'Every 8 h', perDay: 3 },
  { label: 'Every 6 h', perDay: 4 },
  { label: 'Twice daily', perDay: 2 },
  { label: 'Three times daily', perDay: 3 },
  { label: 'As needed', perDay: 1 },
];

/** Auto quantity = doses/day (from the chosen frequency) × duration in days. */
function autoQuantity(frequency: string | undefined, durationDays: number): number {
  const match = FREQUENCY_OPTIONS.find((f) => f.label === frequency);
  const perDay = match?.perDay ?? 1;
  return Math.max(1, perDay * Math.max(1, durationDays));
}

/**
 * Lightweight allergy check against the clinical note's assessment/history text. The note is where
 * allergies are documented on this screen (e.g. "Penicillin allergy"). If the drug family appears
 * in that text, we flag a conflict so the physician sees it before prescribing. This is a safety
 * surface, not a substitute for a full interactions engine.
 */
function conflictsWithAllergies(drugName: string, plan: CarePlan): boolean {
  const noteText = `${plan.note.history ?? ''} ${plan.note.assessment ?? ''} ${plan.note.plan ?? ''}`.toLowerCase();
  if (!noteText.includes('allerg')) return false;
  const name = drugName.toLowerCase();
  // Map a few common drug families so "Penicillin allergy" flags amoxicillin/ampicillin, etc.
  const families: Record<string, string[]> = {
    penicillin: ['penicillin', 'amoxicillin', 'ampicillin', 'flucloxacillin', 'co-amoxiclav'],
    sulfa: ['sulfa', 'sulfamethoxazole', 'cotrimoxazole', 'trimethoprim'],
    cephalosporin: ['cephalosporin', 'cefalexin', 'cephalexin', 'cefuroxime', 'ceftriaxone'],
    nsaid: ['nsaid', 'ibuprofen', 'naproxen', 'diclofenac', 'aspirin'],
    macrolide: ['macrolide', 'azithromycin', 'clarithromycin', 'erythromycin'],
  };
  for (const members of Object.values(families)) {
    const allergyMentioned = members.some((m) => noteText.includes(m));
    const drugInFamily = members.some((m) => name.includes(m));
    if (allergyMentioned && drugInFamily) return true;
  }
  return false;
}

/** A medicine being configured before it is added to the prescription (the "Medicine N" form). */
interface MedDraft {
  medicationId?: string;
  name: string;
  strength?: string;
  form?: string;
  /** true when the drug does not conflict with the patient's known allergies. */
  noAllergyConflict: boolean;
  frequency: string;
  durationDays: number;
  /** null → auto (computed); a number → the physician overrode it. */
  quantity: number | null;
  instructions: string;
}

/**
 * D-08 physician Review / Care plan, styled to the prototype:
 *  - a warm "AI draft" clinical-note card (History/Findings/Assessment/Plan, inline edit)
 *  - ONE white "Care plan · Written by you" card with Diagnosis / Prescription / Follow-up /
 *    Medical leave as rows
 *  - a warm "Summary for the patient" card
 * Diagnosis / medication / diagnostic-test dropdowns come from the DB-backed reference directories.
 * Everything the physician enters is persisted (PUT care-plan) so it can be shared with the patient.
 */
export function CarePlanPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const api = usePhysicianApi();

  const [plan, setPlan] = useState<CarePlan>({ ...EMPTY, appointmentId });
  const [loading, setLoading] = useState(true);
  const [noteEditing, setNoteEditing] = useState<NoteField | null>(null);
  const [summaryEditing, setSummaryEditing] = useState<SummaryField | null>(null);
  const [noteApproved, setNoteApproved] = useState(false);
  const [summaryApproved, setSummaryApproved] = useState(false);

  // Diagnosis/prescription/test "add" open states (the inline search boxes).
  const [addDiagnosis, setAddDiagnosis] = useState(false);
  const [addMedicine, setAddMedicine] = useState(false);
  const [addTest, setAddTest] = useState(false);
  // The medicine being configured before it is committed to the prescription (null = search only).
  const [medDraft, setMedDraft] = useState<MedDraft | null>(null);

  // Follow-up / medical-leave inset editor open states (summary shown when closed).
  const [editFollowUp, setEditFollowUp] = useState(false);
  const [editLeave, setEditLeave] = useState(false);

  useEffect(() => {
    let active = true;
    void api.getCarePlan(appointmentId).then((p) => {
      if (!active) return;
      const loaded = p ? { ...EMPTY, ...p, appointmentId } : { ...EMPTY, appointmentId };
      setPlan(loaded);
      setNoteApproved(loaded.status === 'APPROVED');
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [api, appointmentId]);

  const persist = useCallback(
    async (next: CarePlan) => {
      setPlan(next);
      const saved = await api.saveCarePlan(appointmentId, next);
      if (saved) setPlan({ ...EMPTY, ...saved, appointmentId });
    },
    [api, appointmentId],
  );

  // Deep-link from D-09 "Edit in Review": #section=<id> scrolls to the section and opens its editor.
  useEffect(() => {
    if (loading) return;
    const m = /section=([a-z-]+)/.exec(location.hash);
    const section = m?.[1];
    if (!section) return;
    // Open the relevant inline editor so the physician lands ready to edit.
    if (section === 'note') setNoteEditing('history');
    if (section === 'summary') setSummaryEditing('text');
    if (section === 'follow-up') setEditFollowUp(true);
    if (section === 'medical-leave') setEditLeave(true);
    if (section === 'diagnosis') setAddDiagnosis(true);
    if (section === 'prescription') setAddMedicine(true);
    const el = document.getElementById(`cp-sec-${section}`);
    if (el) {
      window.requestAnimationFrame(() => el.scrollIntoView({ behavior: 'smooth', block: 'start' }));
    }
    // Run once per hash change after data is present.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, location.hash]);

  // Update local state immediately, then persist (debounced via the Done/blur handlers).
  const upd = (patch: Partial<CarePlan>) => setPlan((p) => ({ ...p, ...patch }));
  const updAndSave = (patch: Partial<CarePlan>) => {
    const next = { ...plan, ...patch };
    void persist(next);
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <CarePlanStepper current={0} />
        <div style={{ padding: 24, color: 'var(--bp-muted)' }}>Loading care plan…</div>
      </div>
    );
  }

  const fu = plan.followUp;
  const ml = plan.medicalLeave;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 14 }}>
          <strong style={{ color: 'var(--bp-navy)', fontFamily: 'ui-monospace, Menlo, monospace' }} data-phi="true">{appointmentId}</strong>
        </span>
        <span style={{ flex: 1 }} />
        <CarePlanStepper current={0} />
      </div>

      <h1 className="cp-title">Review</h1>

      {/* ============ Clinical note (warm AI-draft card) ============ */}
      <div className="cp-note-card" id="cp-sec-note">
        <div className="cp-head">
          <span className="cp-head-icon"><Icon name="clinical_notes" size={18} /></span>
          <span className="cp-head-title">Clinical note</span>
          <span className="cp-ai-badge"><Icon name="auto_awesome" size={13} />AI draft · review</span>
          <span style={{ flex: 1 }} />
          <button type="button" className="cp-link" onClick={() => setNoteEditing('history')}>Write manually</button>
          <button
            type="button"
            className={noteApproved ? 'cp-approve is-approved' : 'cp-approve'}
            onClick={() => {
              const next = { ...plan, status: noteApproved ? ('DRAFT' as const) : ('APPROVED' as const) };
              setNoteApproved(!noteApproved);
              void persist(next);
            }}
          >
            <Icon name={noteApproved ? 'verified' : 'task_alt'} size={16} color="#fff" />
            {noteApproved ? 'Approved' : 'Approve'}
          </button>
        </div>
        {NOTE_FIELDS.map(({ key, label }) => (
          <EditRow
            key={key}
            label={label}
            value={plan.note[key]}
            editing={noteEditing === key}
            onEdit={() => setNoteEditing(key)}
            onChange={(v) => upd({ note: { ...plan.note, [key]: v } })}
            onDone={() => { setNoteEditing(null); updAndSave({ note: plan.note }); }}
            emptyText="Not documented"
          />
        ))}
      </div>

      {/* ============ Care plan (single white card, rows) ============ */}
      <div className="cp-plan-card">
        <div className="cp-plan-title">
          <span className="cp-head-icon" style={{ background: 'var(--bp-blue-tint)' }}><Icon name="fact_check" size={18} /></span>
          <span className="cp-plan-title-text">Care plan</span>
          <span className="cp-written-by"><Icon name="edit" size={13} />Written by you</span>
        </div>

        {/* Diagnosis row */}
        <div className="cp-plan-row" id="cp-sec-diagnosis">
          <div className="cp-plan-row-head">
            <span className="cp-head-icon" style={{ width: 28, height: 28, background: 'var(--bp-blue-tint)' }}><Icon name="description" size={16} /></span>
            <span className="cp-plan-row-title">Diagnosis</span>
            {!addDiagnosis && (
              <button type="button" className="cp-add-pill" onClick={() => setAddDiagnosis(true)}>
                <Icon name="add" size={16} color="var(--bp-blue-dark)" />Add
              </button>
            )}
          </div>
          {(plan.diagnoses.length > 0 || addDiagnosis) && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 12 }}>
              {plan.diagnoses.map((d, i) => (
                <div key={d.code} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ font: '600 12px ui-monospace, Menlo, monospace', color: '#fff', background: 'var(--bp-navy)', padding: '3px 8px', borderRadius: 6 }}>{d.code}</span>
                  <span style={{ flex: 1, fontSize: 14, color: 'var(--bp-text)' }}>{d.title}</span>
                  <button type="button" onClick={() => updAndSave({ diagnoses: plan.diagnoses.map((x, xi) => ({ ...x, primary: xi === i })) })}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 12, fontWeight: 600, color: d.primary ? 'var(--bp-blue)' : 'var(--bp-muted)' }}>
                    {d.primary ? 'Primary' : 'Set primary'}
                  </button>
                  <button type="button" className="cp-pencil" aria-label="Remove" onClick={() => updAndSave({ diagnoses: plan.diagnoses.filter((_, xi) => xi !== i) })}>
                    <Icon name="close" size={16} />
                  </button>
                </div>
              ))}
              {addDiagnosis && (
                <div className="cp-medbox">
                  <div className="cp-medbox-title">Diagnosis · ICD-10</div>
                  <SearchDropdown<DiagnosisOption>
                    autoFocus
                    placeholder="Search ICD-10 — code or description"
                    search={(q) => api.searchDiagnoses(q)}
                    renderBadge={(o) => o.code}
                    renderLabel={(o) => o.title}
                    onSelect={(o) => {
                      if (!plan.diagnoses.some((d) => d.code === o.code)) {
                        updAndSave({ diagnoses: [...plan.diagnoses, { code: o.code, title: o.title, primary: plan.diagnoses.length === 0 }] });
                      }
                    }}
                  />
                  <div style={{ marginTop: 10 }}>
                    <button type="button" className="cp-link" onClick={() => setAddDiagnosis(false)}>Done</button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Prescription row */}
        <div className="cp-plan-row" id="cp-sec-prescription">
          <div className="cp-plan-row-head">
            <span className="cp-head-icon" style={{ width: 28, height: 28, background: 'var(--bp-blue-tint)' }}><Icon name="prescriptions" size={16} /></span>
            <span className="cp-plan-row-title">Prescription</span>
          </div>

          {plan.prescriptions.map((m, i) => (
            <div key={i} style={{ marginTop: 12 }}>
              <PrescriptionRow med={m}
                onChange={(patch) => upd({ prescriptions: plan.prescriptions.map((x, xi) => (xi === i ? { ...x, ...patch } : x)) })}
                onBlurSave={() => updAndSave({ prescriptions: plan.prescriptions })}
                onRemove={() => updAndSave({ prescriptions: plan.prescriptions.filter((_, xi) => xi !== i) })} />
            </div>
          ))}

          {/* Diagnostic tests appear under prescription (ordered tests). */}
          {plan.diagnosticTests.map((t, i) => (
            <div key={`t-${i}`} style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10 }}>
              <Icon name="labs" size={16} color="var(--bp-blue)" />
              <span style={{ flex: 1, fontSize: 14, color: 'var(--bp-text)' }}>
                {t.name}
                {t.category || t.specimen ? <span style={{ color: 'var(--bp-muted)' }}> · {[t.category, t.specimen].filter(Boolean).join(' · ')}</span> : null}
              </span>
              <button type="button" className="cp-pencil" aria-label="Remove" onClick={() => updAndSave({ diagnosticTests: plan.diagnosticTests.filter((_, xi) => xi !== i) })}>
                <Icon name="close" size={16} />
              </button>
            </div>
          ))}

          {addMedicine && (
            <div className="cp-medbox">
              <div className="cp-medbox-title">Medicine {plan.prescriptions.length + 1}</div>
              {!medDraft ? (
                // Step 1 — pick the drug from the directory. Picking does NOT add it yet.
                <>
                  <SearchDropdown<MedicationOption>
                    autoFocus
                    placeholder="Search medication — try amoxicillin"
                    search={(q) => api.searchMedications(q)}
                    renderLabel={(o) => `${o.name}${o.strength ? ` · ${o.strength}` : ''}`}
                    renderSub={(o) => o.form ?? undefined}
                    onSelect={(o) =>
                      setMedDraft({
                        medicationId: o.id,
                        name: o.name,
                        strength: o.strength ?? undefined,
                        form: o.form ?? undefined,
                        noAllergyConflict: !conflictsWithAllergies(o.name, plan),
                        frequency: FREQUENCY_OPTIONS[0].label,
                        durationDays: 5,
                        quantity: null,
                        instructions: '',
                      })
                    }
                  />
                  <div style={{ marginTop: 10 }}>
                    <button type="button" className="cp-link" onClick={() => setAddMedicine(false)}>Cancel</button>
                  </div>
                </>
              ) : (
                // Step 2 — configure dose/frequency/duration/quantity/instructions, then Add.
                <MedicineForm
                  draft={medDraft}
                  onChange={(patch) => setMedDraft((d) => (d ? { ...d, ...patch } : d))}
                  onChangeDrug={() => setMedDraft(null)}
                  onCancel={() => { setMedDraft(null); setAddMedicine(false); }}
                  onAdd={() => {
                    const qty = medDraft.quantity ?? autoQuantity(medDraft.frequency, medDraft.durationDays);
                    updAndSave({
                      prescriptions: [
                        ...plan.prescriptions,
                        {
                          medicationId: medDraft.medicationId,
                          name: medDraft.name,
                          strength: medDraft.strength,
                          form: medDraft.form,
                          frequency: medDraft.frequency,
                          durationDays: medDraft.durationDays,
                          quantity: qty,
                          instructions: medDraft.instructions.trim() || undefined,
                        },
                      ],
                    });
                    setMedDraft(null);
                    setAddMedicine(false);
                  }}
                />
              )}
            </div>
          )}
          {addTest && (
            <div className="cp-medbox">
              <div className="cp-medbox-title">Diagnostic test</div>
              <SearchDropdown<DiagnosticTestOption>
                autoFocus
                placeholder="Search diagnostic test"
                search={(q) => api.searchDiagnosticTests(q)}
                renderLabel={(o) => o.name}
                renderSub={(o) => [o.category, o.specimen].filter(Boolean).join(' · ') || undefined}
                onSelect={(o) => {
                  if (!plan.diagnosticTests.some((t) => t.testId === o.id)) {
                    updAndSave({ diagnosticTests: [...plan.diagnosticTests, { testId: o.id, name: o.name, category: o.category ?? undefined, specimen: o.specimen ?? undefined }] });
                  }
                  setAddTest(false);
                }}
              />
              <div style={{ marginTop: 10 }}>
                <button type="button" className="cp-link" onClick={() => setAddTest(false)}>Cancel</button>
              </div>
            </div>
          )}

          {!addMedicine && !addTest && (
            <div className="cp-add-split">
              <button type="button" onClick={() => setAddMedicine(true)}><Icon name="add" size={16} color="var(--bp-blue)" />Add medication</button>
              <button type="button" onClick={() => setAddTest(true)}><Icon name="add" size={16} color="var(--bp-blue)" />Add diagnostic test</button>
            </div>
          )}
        </div>

        {/* Follow-up row */}
        <div className="cp-plan-row" id="cp-sec-follow-up">
          <div className="cp-plan-row-head">
            <span className="cp-head-icon" style={{ width: 28, height: 28, background: 'var(--bp-blue-tint)' }}><Icon name="event_repeat" size={16} /></span>
            <span className="cp-plan-row-title">Follow-up</span>
            <Switch
              on={fu.enabled}
              onToggle={(v) => {
                setEditFollowUp(v);
                updAndSave({ followUp: { ...fu, enabled: v, mode: v ? (fu.mode ?? 'Video check-in') : fu.mode, inDays: v ? (fu.inDays ?? 5) : fu.inDays, date: v ? (fu.date ?? todayPlus(fu.inDays ?? 5)) : fu.date } });
              }}
            />
          </div>

          {fu.enabled && !editFollowUp && (
            <div className="cp-rowsum">
              <div className="cp-rowsum-body">
                <div className="cp-rowsum-title">{fu.mode?.trim() || 'Video check-in'}</div>
                <div className="cp-rowsum-sub">
                  {fu.inDays ? `In ${fu.inDays} day${fu.inDays === 1 ? '' : 's'}` : 'Scheduled'}
                  {fu.date ? ` · ${fmtShortDate(fu.date)}` : ''}
                </div>
              </div>
              <button type="button" className="cp-pencil-btn" aria-label="Edit follow-up" onClick={() => setEditFollowUp(true)}>
                <Icon name="edit" size={17} />
              </button>
            </div>
          )}

          {fu.enabled && editFollowUp && (
            <div className="cp-inset">
              <div className="cp-inset-title">Follow-up</div>
              <span className="cp-inset-label">When</span>
              <div className="cp-presets" role="group" aria-label="Follow-up in">
                {FOLLOW_UP_PRESETS.map((p) => {
                  const active = (fu.inDays ?? 0) === p.days;
                  return (
                    <button
                      key={`${p.num}-${p.unit}`}
                      type="button"
                      className={active ? 'cp-preset is-active' : 'cp-preset'}
                      aria-pressed={active}
                      onClick={() => upd({ followUp: { ...fu, inDays: p.days, date: todayPlus(p.days) } })}
                    >
                      <span className="cp-preset-num">{p.num}</span>
                      <span className="cp-preset-unit">{p.unit}</span>
                    </button>
                  );
                })}
              </div>
              <button type="button" className="cp-done" onClick={() => { setEditFollowUp(false); updAndSave({ followUp: plan.followUp }); }}>Done</button>
            </div>
          )}
        </div>

        {/* Medical leave row */}
        <div className="cp-plan-row" id="cp-sec-medical-leave">
          <div className="cp-plan-row-head">
            <span className="cp-head-icon" style={{ width: 28, height: 28, background: 'var(--bp-blue-tint)' }}><Icon name="event_busy" size={16} /></span>
            <span className="cp-plan-row-title">Medical leave</span>
            <Switch
              on={ml.enabled}
              onToggle={(v) => {
                setEditLeave(v);
                const start = ml.startDate ?? todayPlus(0);
                updAndSave({ medicalLeave: { ...ml, enabled: v, reason: v ? (ml.reason ?? LEAVE_TYPES[0]) : ml.reason, startDate: v ? start : ml.startDate, endDate: v ? (ml.endDate ?? endFrom(start, 2)) : ml.endDate } });
              }}
            />
          </div>

          {ml.enabled && !editLeave && (
            <div className="cp-rowsum">
              <div className="cp-rowsum-body">
                <div className="cp-rowsum-title">{ml.reason?.trim() || LEAVE_TYPES[0]}</div>
                <div className="cp-rowsum-sub">
                  {(() => {
                    const days = inclusiveDays(ml.startDate, ml.endDate);
                    const range = ml.startDate && ml.endDate ? ` · ${fmtShortDate(ml.startDate)} → ${fmtShortDate(ml.endDate)}` : '';
                    return `${days} day${days === 1 ? '' : 's'}${range}`;
                  })()}
                </div>
              </div>
              <button type="button" className="cp-pencil-btn" aria-label="Edit medical leave" onClick={() => setEditLeave(true)}>
                <Icon name="edit" size={17} />
              </button>
            </div>
          )}

          {ml.enabled && editLeave && (
            <div className="cp-inset">
              <div className="cp-inset-title">Medical leave</div>

              <span className="cp-inset-label">Type</span>
              <select
                className="cp-select"
                value={ml.reason ?? LEAVE_TYPES[0]}
                onChange={(e) => updAndSave({ medicalLeave: { ...plan.medicalLeave, reason: e.target.value } })}
              >
                {LEAVE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>

              <div className="cp-inset-grid" style={{ marginTop: 14 }}>
                <div>
                  <span className="cp-inset-label" style={{ marginTop: 0 }}>Start date</span>
                  <input
                    className="cp-date"
                    type="date"
                    value={ml.startDate ?? ''}
                    onChange={(e) => {
                      const start = e.target.value;
                      const days = inclusiveDays(ml.startDate, ml.endDate);
                      updAndSave({ medicalLeave: { ...plan.medicalLeave, startDate: start, endDate: start ? endFrom(start, days) : plan.medicalLeave.endDate } });
                    }}
                  />
                </div>
                <div>
                  <span className="cp-inset-label" style={{ marginTop: 0 }}>Days</span>
                  <DaysStepper
                    value={inclusiveDays(ml.startDate, ml.endDate)}
                    min={1}
                    onChange={(days) => {
                      const start = ml.startDate ?? todayPlus(0);
                      updAndSave({ medicalLeave: { ...plan.medicalLeave, startDate: start, endDate: endFrom(start, days) } });
                    }}
                  />
                </div>
              </div>

              <button type="button" className="cp-done" onClick={() => { setEditLeave(false); updAndSave({ medicalLeave: plan.medicalLeave }); }}>Done</button>
            </div>
          )}
        </div>
      </div>

      {/* ============ Summary (warm AI-draft card) ============ */}
      <div className="cp-note-card" id="cp-sec-summary">
        <div className="cp-head">
          <span className="cp-head-icon"><Icon name="summarize" size={18} /></span>
          <span className="cp-head-title">Summary for the patient</span>
          <span className="cp-ai-badge"><Icon name="auto_awesome" size={13} />AI draft · review</span>
          <span style={{ flex: 1 }} />
          <button type="button" className="cp-link" onClick={() => setSummaryEditing('text')}>Write manually</button>
          <button
            type="button"
            className={summaryApproved ? 'cp-approve is-approved' : 'cp-approve'}
            onClick={() => setSummaryApproved((v) => !v)}
          >
            <Icon name={summaryApproved ? 'verified' : 'task_alt'} size={16} color="#fff" />
            {summaryApproved ? 'Approved' : 'Approve'}
          </button>
        </div>
        <EditRow
          label="Summary"
          value={plan.summary.text}
          editing={summaryEditing === 'text'}
          onEdit={() => setSummaryEditing('text')}
          onChange={(v) => upd({ summary: { ...plan.summary, text: v } })}
          onDone={() => { setSummaryEditing(null); updAndSave({ summary: plan.summary }); }}
          emptyText="Not written"
        />
        <EditRow
          label="Instructions"
          value={plan.summary.instructions}
          editing={summaryEditing === 'instructions'}
          onEdit={() => setSummaryEditing('instructions')}
          onChange={(v) => upd({ summary: { ...plan.summary, instructions: v } })}
          onDone={() => { setSummaryEditing(null); updAndSave({ summary: plan.summary }); }}
          emptyText="Not written"
        />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
        <BpButton
          icon="arrow_forward"
          disabled={!noteApproved}
          onClick={() => void persist(plan).then(() => navigate(`/consultation/${appointmentId}/send`))}
        >
          Continue to send
        </BpButton>
        {!noteApproved && (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--bp-muted)' }}>
            <Icon name="info" size={16} color="var(--bp-muted)" />
            Approve the clinical note to continue.
          </span>
        )}
      </div>
    </div>
  );
}

/** A two-column note/summary row with an inline pencil → textarea → check. */
function EditRow({
  label,
  value,
  editing,
  onEdit,
  onChange,
  onDone,
  emptyText,
}: {
  label: string;
  value?: string;
  editing: boolean;
  onEdit: () => void;
  onChange: (v: string) => void;
  onDone: () => void;
  emptyText: string;
}) {
  return (
    <div className="cp-row">
      <span className="cp-row-label">{label}</span>
      {editing ? (
        <textarea
          className="cp-edit-field"
          autoFocus
          rows={3}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
          data-phi="true"
        />
      ) : (
        <span className={value && value.trim() ? 'cp-row-value' : 'cp-row-value is-empty'} data-phi="true">
          {value && value.trim() ? value : emptyText}
        </span>
      )}
      {editing ? (
        <button type="button" className="cp-pencil" aria-label="Done" title="Done" onClick={onDone}><Icon name="check" size={18} color="var(--bp-blue)" /></button>
      ) : (
        <button type="button" className="cp-pencil" aria-label={`Edit ${label}`} title={`Edit ${label}`} onClick={onEdit}><Icon name="edit" size={16} /></button>
      )}
    </div>
  );
}

function Switch({ on, onToggle }: { on: boolean; onToggle: (v: boolean) => void }) {
  return (
    <button type="button" className={on ? 'cp-switch on' : 'cp-switch'} aria-pressed={on} aria-label="Toggle" onClick={() => onToggle(!on)}>
      <span className="cp-switch-knob" />
    </button>
  );
}

/** −/+ numeric stepper used for the medical-leave day count. */
function DaysStepper({ value, min = 1, onChange }: { value: number; min?: number; onChange: (v: number) => void }) {
  return (
    <div className="cp-stepper">
      <button type="button" className="cp-stepper-btn" aria-label="Fewer days" disabled={value <= min} onClick={() => onChange(Math.max(min, value - 1))}>
        <Icon name="remove" size={18} />
      </button>
      <span className="cp-stepper-val"><strong>{value}</strong><span className="cp-stepper-unit">days</span></span>
      <button type="button" className="cp-stepper-btn" aria-label="More days" onClick={() => onChange(value + 1)}>
        <Icon name="add" size={18} />
      </button>
    </div>
  );
}

function PrescriptionRow({
  med,
  onChange,
  onBlurSave,
  onRemove,
}: {
  med: CarePlanPrescription;
  onChange: (patch: Partial<CarePlanPrescription>) => void;
  onBlurSave: () => void;
  onRemove: () => void;
}) {
  const title = `${med.name}${med.strength ? ` · ${med.strength}` : ''}${med.form ? ` ${med.form}` : ''}`;
  const sub = [med.dosage, med.frequency, med.durationDays ? `${med.durationDays} days` : null, med.quantity ? `Qty ${med.quantity}` : null]
    .filter(Boolean)
    .join(' · ');
  const [open, setOpen] = useState(false);
  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--bp-text)' }}>{title}</div>
          {sub && <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{sub}</div>}
        </div>
        <button type="button" className="cp-pencil" aria-label="Edit" onClick={() => setOpen((v) => !v)}><Icon name="edit" size={16} /></button>
        <button type="button" className="cp-pencil" aria-label="Remove" onClick={onRemove}><Icon name="close" size={16} /></button>
      </div>
      {open && (
        <div className="cp-medbox" style={{ marginTop: 10 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <Field label="Dosage"><input className="cp-edit-field" value={med.dosage ?? ''} placeholder="1 tablet" onChange={(e) => onChange({ dosage: e.target.value })} onBlur={onBlurSave} /></Field>
            <Field label="Frequency"><input className="cp-edit-field" value={med.frequency ?? ''} placeholder="Once daily" onChange={(e) => onChange({ frequency: e.target.value })} onBlur={onBlurSave} /></Field>
            <Field label="Duration (days)"><input className="cp-edit-field" type="number" value={med.durationDays ?? ''} onChange={(e) => onChange({ durationDays: e.target.value ? Number(e.target.value) : null })} onBlur={onBlurSave} /></Field>
            <Field label="Quantity"><input className="cp-edit-field" type="number" value={med.quantity ?? ''} onChange={(e) => onChange({ quantity: e.target.value ? Number(e.target.value) : null })} onBlur={onBlurSave} /></Field>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Instructions"><input className="cp-edit-field" value={med.instructions ?? ''} onChange={(e) => onChange({ instructions: e.target.value })} onBlur={onBlurSave} /></Field>
            </div>
          </div>
          <div style={{ marginTop: 10 }}><button type="button" className="cp-link" onClick={() => setOpen(false)}>Done</button></div>
        </div>
      )}
    </div>
  );
}

/**
 * The "Medicine N" configuration form shown after a drug is picked but before it is added:
 * selected drug + allergy check + Change, Frequency, Duration stepper, Quantity (AUTO), Instructions,
 * then Add medication / Cancel. Matches the D-08 prototype.
 */
function MedicineForm({
  draft,
  onChange,
  onChangeDrug,
  onCancel,
  onAdd,
}: {
  draft: MedDraft;
  onChange: (patch: Partial<MedDraft>) => void;
  onChangeDrug: () => void;
  onCancel: () => void;
  onAdd: () => void;
}) {
  const autoQty = autoQuantity(draft.frequency, draft.durationDays);
  const qtyValue = draft.quantity ?? autoQty;
  const isAuto = draft.quantity === null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Selected drug + allergy status + Change */}
      <div className="cp-med-selected">
        <div>
          <div className="cp-med-selected-name">{draft.name}</div>
          <div className="cp-med-selected-sub">
            {draft.strength ? `${draft.strength} ` : ''}{draft.form ?? ''}
            {draft.noAllergyConflict ? (
              <span className="cp-allergy-ok"><Icon name="check_circle" size={14} />No allergy conflict</span>
            ) : (
              <span className="cp-allergy-warn"><Icon name="warning" size={14} />Possible allergy conflict</span>
            )}
          </div>
        </div>
        <button type="button" className="cp-link" style={{ color: 'var(--bp-blue)' }} onClick={onChangeDrug}>Change</button>
      </div>

      {/* Frequency */}
      <div>
        <span className="cp-inset-label" style={{ marginTop: 0 }}>Frequency</span>
        <select className="cp-select" value={draft.frequency} onChange={(e) => onChange({ frequency: e.target.value })}>
          {FREQUENCY_OPTIONS.map((f) => <option key={f.label} value={f.label}>{f.label}</option>)}
        </select>
      </div>

      {/* Duration + Quantity */}
      <div className="cp-inset-grid">
        <div>
          <span className="cp-inset-label" style={{ marginTop: 0 }}>Duration</span>
          <div className="cp-stepper">
            <button type="button" className="cp-stepper-btn" aria-label="Fewer days" disabled={draft.durationDays <= 1}
              onClick={() => onChange({ durationDays: Math.max(1, draft.durationDays - 1) })}>
              <Icon name="remove" size={18} />
            </button>
            <span className="cp-stepper-val"><strong>{draft.durationDays}</strong><span className="cp-stepper-unit">days</span></span>
            <button type="button" className="cp-stepper-btn" aria-label="More days"
              onClick={() => onChange({ durationDays: draft.durationDays + 1 })}>
              <Icon name="add" size={18} />
            </button>
          </div>
        </div>
        <div>
          <span className="cp-inset-label" style={{ marginTop: 0 }}>Quantity</span>
          <div className="cp-qty">
            <input
              className="cp-qty-input"
              type="number"
              min={1}
              value={qtyValue}
              onChange={(e) => onChange({ quantity: e.target.value ? Number(e.target.value) : null })}
            />
            {isAuto && <span className="cp-qty-auto">AUTO</span>}
          </div>
        </div>
      </div>

      {/* Instructions */}
      <div>
        <span className="cp-inset-label" style={{ marginTop: 0 }}>Instructions for the patient</span>
        <input
          className="cp-edit-field"
          value={draft.instructions}
          placeholder="e.g. Take with food · finish the full course"
          onChange={(e) => onChange({ instructions: e.target.value })}
        />
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <button type="button" className="cp-done" style={{ marginTop: 0 }} onClick={onAdd}>Add medication</button>
        <button type="button" className="cp-link" onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { MedicationEntry, PhysicianDocumentationPayload } from '@telemedicine/shared-types';
import { usePhysicianApi } from '../api/physicianApi';
import { labels } from '../config/labels';
import {
  BpButton,
  BpInput,
  BpTextArea,
  Card,
  Field,
  Icon,
  Pill,
  SectionHeader,
} from '../ui/primitives';
import { MOCK_CONTEXTS } from '../data/mockData';

const AUTOSAVE_MS = 30_000;
type DocTab = 'diagnosis' | 'prescription' | 'medicalLeave';
const draftKey = (id: string) => `physician-doc-draft-${id}`;

function seedDraft(id: string): PhysicianDocumentationPayload {
  const ctx = MOCK_CONTEXTS[id];
  if (id === 'AP-20931') {
    return {
      diagnosis: { icd10: 'J02.9', notes: 'Acute pharyngitis resolved. Favourable progression, afebrile 72 h.' },
      prescription: { medications: [{ drugName: 'Paracetamol 1g', dosage: '1 tablet', frequency: 'q8h PRN for 3 days' }] },
      medicalLeave: {},
    };
  }
  return { diagnosis: { icd10: '', notes: ctx?.reason ?? '' }, prescription: { medications: [{ drugName: '', dosage: '', frequency: '' }] } };
}

function loadDraft(id: string): PhysicianDocumentationPayload {
  try {
    const raw = sessionStorage.getItem(draftKey(id));
    return raw ? (JSON.parse(raw) as PhysicianDocumentationPayload) : seedDraft(id);
  } catch {
    return seedDraft(id);
  }
}

const TABS: { id: DocTab; label: string; icon: string }[] = [
  { id: 'diagnosis', label: labels.documentation.diagnosis, icon: 'clinical_notes' },
  { id: 'prescription', label: labels.documentation.prescription, icon: 'prescriptions' },
  { id: 'medicalLeave', label: labels.documentation.medicalLeave, icon: 'event_busy' },
];

export function DocumentationPage() {
  const { appointmentId = 'apt-demo' } = useParams<{ appointmentId: string }>();
  const api = usePhysicianApi();
  const [activeTab, setActiveTab] = useState<DocTab>('diagnosis');
  const [draft, setDraft] = useState<PhysicianDocumentationPayload>(() => loadDraft(appointmentId));
  const [submitting, setSubmitting] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);

  useEffect(() => setDraft(loadDraft(appointmentId)), [appointmentId]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      sessionStorage.setItem(draftKey(appointmentId), JSON.stringify(draft));
      setSaved(new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));
    }, AUTOSAVE_MS);
    return () => window.clearInterval(timer);
  }, [appointmentId, draft]);

  const setDiag = (f: 'icd10' | 'notes', v: string) => setDraft((p) => ({ ...p, diagnosis: { ...p.diagnosis, [f]: v } }));
  const setMed = (i: number, f: keyof MedicationEntry, v: string) =>
    setDraft((p) => {
      const meds = [...(p.prescription?.medications ?? [{ drugName: '', dosage: '', frequency: '' }])];
      meds[i] = { ...meds[i], [f]: v };
      return { ...p, prescription: { medications: meds } };
    });
  const addMed = () =>
    setDraft((p) => ({ ...p, prescription: { medications: [...(p.prescription?.medications ?? []), { drugName: '', dosage: '', frequency: '' }] } }));
  const setLeave = (f: 'startDate' | 'endDate' | 'notes', v: string) =>
    setDraft((p) => ({ ...p, medicalLeave: { ...p.medicalLeave, [f]: v } }));

  const meds = useMemo(() => draft.prescription?.medications ?? [{ drugName: '', dosage: '', frequency: '' }], [draft]);

  const submit = async () => {
    setSubmitting(true);
    try {
      sessionStorage.setItem(draftKey(appointmentId), JSON.stringify(draft));
      await api.submitDocumentation(appointmentId, draft);
      setSaved('submitted');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title={labels.documentation.title}
        subtitle={<span style={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{appointmentId}</span>}
        actions={
          <>
            {saved && (
              <Pill tone="green" icon="cloud_done">
                {saved === 'submitted' ? labels.documentation.submitted : `${labels.documentation.savedDraft} ${saved}`}
              </Pill>
            )}
            <Link to={`/consultation/${appointmentId}`}>
              <BpButton variant="ghost" icon="arrow_back">
                {labels.workspace.title}
              </BpButton>
            </Link>
          </>
        }
      />

      <Card padded={false}>
        <div style={{ display: 'flex', gap: 4, padding: '6px 6px 0', borderBottom: '1px solid var(--bp-border-2)' }}>
          {TABS.map((tab) => {
            const on = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 7,
                  padding: '10px 16px',
                  border: 'none',
                  background: 'none',
                  cursor: 'pointer',
                  fontWeight: on ? 700 : 500,
                  color: on ? 'var(--bp-blue-dark)' : 'var(--bp-muted)',
                  borderBottom: `2px solid ${on ? 'var(--bp-blue)' : 'transparent'}`,
                  font: 'inherit',
                }}
              >
                <Icon name={tab.icon} size={18} />
                {tab.label}
              </button>
            );
          })}
        </div>

        <div style={{ padding: 20 }}>
          {activeTab === 'diagnosis' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Field label={labels.documentation.icd10}>
                <BpInput value={draft.diagnosis?.icd10 ?? ''} onChange={(e) => setDiag('icd10', e.target.value)} placeholder="Ej. J02.9" />
              </Field>
              <Field label={labels.documentation.notes}>
                <BpTextArea value={draft.diagnosis?.notes ?? ''} onChange={(e) => setDiag('notes', e.target.value)} rows={5} />
              </Field>
            </div>
          )}

          {activeTab === 'prescription' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              {meds.map((med, i) => (
                <div key={i} className="grid-3col" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1.4fr', gap: 10, padding: 12, background: 'var(--bp-surface-2)', borderRadius: 8 }}>
                  <Field label={labels.documentation.drugName}>
                    <BpInput value={med.drugName} onChange={(e) => setMed(i, 'drugName', e.target.value)} />
                  </Field>
                  <Field label={labels.documentation.dosage}>
                    <BpInput value={med.dosage} onChange={(e) => setMed(i, 'dosage', e.target.value)} />
                  </Field>
                  <Field label={labels.documentation.frequency}>
                    <BpInput value={med.frequency} onChange={(e) => setMed(i, 'frequency', e.target.value)} />
                  </Field>
                </div>
              ))}
              <div>
                <BpButton variant="secondary" icon="add" onClick={addMed}>
                  {labels.documentation.addMedication}
                </BpButton>
              </div>
            </div>
          )}

          {activeTab === 'medicalLeave' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="grid-2col" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label={labels.documentation.startDate}>
                  <BpInput type="date" value={draft.medicalLeave?.startDate ?? ''} onChange={(e) => setLeave('startDate', e.target.value)} />
                </Field>
                <Field label={labels.documentation.endDate}>
                  <BpInput type="date" value={draft.medicalLeave?.endDate ?? ''} onChange={(e) => setLeave('endDate', e.target.value)} />
                </Field>
              </div>
              <Field label={labels.documentation.notes}>
                <BpTextArea value={draft.medicalLeave?.notes ?? ''} onChange={(e) => setLeave('notes', e.target.value)} rows={3} />
              </Field>
            </div>
          )}
        </div>

        <div style={{ padding: 16, borderTop: '1px solid var(--bp-border-2)', display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <BpButton icon="send" loading={submitting} onClick={() => void submit()}>
            {labels.documentation.submit}
          </BpButton>
        </div>
      </Card>
    </div>
  );
}

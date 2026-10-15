import { Link, useNavigate, useParams } from 'react-router-dom';
import { labels } from '../config/labels';
import { Avatar, BpButton, Card, Icon, Overline, Pill, SectionHeader } from '../ui/primitives';
import { MOCK_CONTEXTS } from '../data/mockData';

function initials(name: string) {
  return name
    .replace(/\(.*?\)/g, '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

function InfoRow({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <div style={{ display: 'flex', gap: 12, padding: '10px 0', borderBottom: '1px solid var(--bp-border-2)' }}>
      <Icon name={icon} size={20} color="var(--bp-blue)" />
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 12, color: 'var(--bp-muted)' }}>{label}</div>
        <div style={{ fontWeight: 600, marginTop: 2 }} data-phi="true">
          {value}
        </div>
      </div>
    </div>
  );
}

function ListCard({ icon, title, items, tone }: { icon: string; title: string; items: string[]; tone: 'blue' | 'green' | 'amber' }) {
  const bg = { blue: 'var(--bp-blue-tint)', green: 'var(--bp-ok-bg)', amber: 'var(--bp-warn-bg)' }[tone];
  const fg = { blue: 'var(--bp-blue-dark)', green: 'var(--bp-ok)', amber: 'var(--bp-warn)' }[tone];
  return (
    <Card>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span style={{ width: 28, height: 28, borderRadius: 7, background: bg, color: fg, display: 'grid', placeItems: 'center' }}>
          <Icon name={icon} size={18} />
        </span>
        <strong style={{ fontSize: 14 }}>{title}</strong>
      </div>
      <ul style={{ margin: 0, paddingLeft: 18, color: 'var(--bp-text-2)', fontSize: 14, lineHeight: 1.7 }} data-phi="true">
        {items.map((it, i) => (
          <li key={i}>{it}</li>
        ))}
      </ul>
    </Card>
  );
}

export function PatientContextPage() {
  const { appointmentId = '' } = useParams<{ appointmentId: string }>();
  const navigate = useNavigate();
  const ctx = MOCK_CONTEXTS[appointmentId];

  if (!ctx) {
    return (
      <Card style={{ textAlign: 'center', padding: 40 }}>
        <Icon name="cloud_off" size={32} color="var(--bp-muted-2)" />
        <p style={{ color: 'var(--bp-muted)' }}>{labels.context.hisUnavailable}</p>
        <Link to="/worklist">
          <BpButton variant="secondary" icon="arrow_back">
            {labels.common.back}
          </BpButton>
        </Link>
      </Card>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <SectionHeader
        title="Before you join"
        subtitle="Everything about the patient before entering: reason, history, consents and readiness."
        actions={
          <>
            <Link to="/worklist">
              <BpButton variant="ghost" icon="arrow_back">
                {labels.common.back}
              </BpButton>
            </Link>
            <BpButton icon="login" onClick={() => navigate(`/consultation/${appointmentId}`)}>
              {labels.worklist.admit}
            </BpButton>
          </>
        }
      />

      {ctx.redFlag && (
        <Card style={{ borderLeft: '4px solid var(--bp-danger)', background: 'var(--bp-danger-bg)' }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <Icon name="warning" size={24} color="var(--bp-danger)" fill />
            <div>
              <strong style={{ color: 'var(--bp-danger)' }}>Red flag — acknowledge before joining</strong>
              <p style={{ margin: '4px 0 0', color: 'var(--bp-text-2)' }}>{ctx.redFlag}</p>
            </div>
          </div>
        </Card>
      )}

      {ctx.actsFor && (
        <Card style={{ borderLeft: '4px solid var(--bp-blue)', background: 'var(--bp-blue-tint-2)' }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Icon name="supervisor_account" size={22} color="var(--bp-blue-dark)" />
            <div>
              <strong>Dependant consultation</strong>
              <p style={{ margin: '2px 0 0', color: 'var(--bp-text-2)', fontSize: 14 }}>{ctx.actsFor}</p>
            </div>
          </div>
        </Card>
      )}

      {ctx.linkedEncounter && (
        <Card style={{ borderLeft: '4px solid var(--bp-ok-2)', background: 'var(--bp-ok-bg)' }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Icon name="link" size={22} color="var(--bp-ok)" />
            <div>
              <strong>Linked follow-up</strong>
              <p style={{ margin: '2px 0 0', color: 'var(--bp-text-2)', fontSize: 14 }}>{ctx.linkedEncounter}</p>
            </div>
          </div>
        </Card>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: 18, alignItems: 'start' }}>
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <Avatar initials={initials(ctx.name)} size={48} tone="navy" />
            <div>
              <strong style={{ fontSize: 16 }} data-phi="true">
                {ctx.name}
              </strong>
              <div style={{ color: 'var(--bp-muted)', fontSize: 13 }} data-phi="true">
                {ctx.age} yrs · {ctx.sex}
              </div>
            </div>
          </div>
          <InfoRow icon="badge" label="RUT" value={ctx.rut} />
          <InfoRow icon="shield" label={labels.context.insurance} value={ctx.insurance} />
          <InfoRow icon="task_alt" label="Readiness" value={ctx.readiness} />
          {ctx.lastVisit && <InfoRow icon="history" label="Last visit" value={ctx.lastVisit} />}
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <Card>
            <Overline>Reason for consultation</Overline>
            <p style={{ margin: '8px 0 0', fontSize: 15, lineHeight: 1.6 }} data-phi="true">
              {ctx.reason}
            </p>
            <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <Pill tone="green" icon="verified">
                Platform consent
              </Pill>
              <Pill tone="green" icon="verified">
                Clinical consent
              </Pill>
              <Pill tone="blue" icon="lock">
                Identity verified
              </Pill>
            </div>
          </Card>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 18 }}>
            <ListCard icon="medication" title={labels.context.medications} items={ctx.medications} tone="blue" />
            <ListCard icon="warning_amber" title={labels.context.allergies} items={ctx.allergies} tone="amber" />
            <ListCard icon="clinical_notes" title={labels.context.history} items={ctx.conditions} tone="green" />
          </div>
        </div>
      </div>
    </div>
  );
}

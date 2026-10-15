import { Link, useLocation } from 'react-router-dom';
import { Stepper } from '@telemedicine/design-system';
import { useSession } from '@telemedicine/session-context';
import { useTranslation } from '@telemedicine/i18n';
import type { MfeRemote } from '@telemedicine/shared-types';
import { hasReachedStatus } from '@telemedicine/shared-types';

const JOURNEY_STEPS = [
  { route: '/patient/consent', labelKey: 'consent', requiredStatus: 'LINK_GENERATED' as const },
  { route: '/patient/identity', labelKey: 'identity', requiredStatus: 'CONSENT_GRANTED' as const },
  { route: '/patient/waiting-room', labelKey: 'waitingRoom', requiredStatus: 'IDENTITY_VALIDATED' as const },
  { route: '/patient/consultation', labelKey: 'consultation', requiredStatus: 'OPEN_DOCTOR' as const },
  { route: '/patient/documents', labelKey: 'documents', requiredStatus: 'CONSULTATION_CLOSED' as const },
  { route: '/patient/survey', labelKey: 'survey', requiredStatus: 'CONSULTATION_CLOSED' as const },
];

export function PatientJourneyNav({ remotes }: { remotes: MfeRemote[] }) {
  const { state } = useSession();
  const location = useLocation();
  const { t } = useTranslation('patient');

  const stepLabels = JOURNEY_STEPS.map((s) => t(s.labelKey));
  const currentStep = Math.max(
    0,
    JOURNEY_STEPS.findIndex((s) => location.pathname.startsWith(s.route)),
  );

  const inConsultation = state.journeyStatus === 'IN_CONSULTATION';

  return (
    <div style={{ marginBottom: '1.5rem' }}>
      <Stepper steps={stepLabels} currentStep={currentStep >= 0 ? currentStep : 0} />
      <nav
        aria-label="Patient journey"
        style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginTop: '1rem' }}
      >
        <Link to="/">Home</Link>
        {JOURNEY_STEPS.map((step) => {
          const remote = remotes.find((r) => r.route === step.route);
          if (!remote) return null;
          const enabled = hasReachedStatus(state.journeyStatus, step.requiredStatus);
          const blocked = inConsultation && !step.route.includes('consultation');
          if (blocked) {
            return (
              <span key={step.route} aria-disabled="true" style={{ opacity: 0.5 }}>
                {t(step.labelKey)}
              </span>
            );
          }
          return enabled ? (
            <Link key={step.route} to={step.route} aria-current={location.pathname.startsWith(step.route) ? 'page' : undefined}>
              {t(step.labelKey)}
            </Link>
          ) : (
            <span key={step.route} aria-disabled="true" style={{ opacity: 0.5 }}>
              {t(step.labelKey)}
            </span>
          );
        })}
      </nav>
    </div>
  );
}

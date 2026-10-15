import { useCallback, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import { useSession } from '@telemedicine/session-context';
import type { ConsentStatus, ConsentType } from '@telemedicine/shared-types';
import consentText from './consent-text';

export interface UseConsentOptions {
  appointmentId?: string;
}

export function useConsent(options: UseConsentOptions = {}) {
  const client = useApiClient();
  const { state, updateMetadata } = useSession();
  const appointmentId = options.appointmentId ?? state.appointmentId;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const platformStatus = (state.metadata.platformConsentStatus as ConsentStatus) ?? 'pending';
  const clinicalStatus = (state.metadata.clinicalConsentStatus as ConsentStatus) ?? 'pending';

  const grantConsent = useCallback(
    async (type: ConsentType) => {
      if (!appointmentId) throw new Error('No appointment ID');
      setLoading(true);
      setError(null);
      try {
        const version =
          type === 'platform' ? consentText.platform.version : consentText.clinical.version;
        await client.post(`/v1/waiting-room/consent/${type}`, {
          appointmentId,
          version,
        }, { idempotencyKey: crypto.randomUUID() });
        const statusKey =
          type === 'platform' ? 'platformConsentStatus' : 'clinicalConsentStatus';
        updateMetadata({ [statusKey]: 'granted' satisfies ConsentStatus });
        if (type === 'clinical') {
          updateMetadata({ clinicalConsentGranted: true });
        }
      } catch (err) {
        setError(err instanceof Error ? err : new Error(String(err)));
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [appointmentId, client, updateMetadata],
  );

  const withdrawConsent = useCallback(async () => {
    if (!appointmentId) throw new Error('No appointment ID');
    setLoading(true);
    setError(null);
    try {
      await client.post(
        '/v1/waiting-room/consent/withdraw',
        { appointmentId },
        { idempotencyKey: crypto.randomUUID() },
      );
      updateMetadata({
        platformConsentStatus: 'withdrawn' satisfies ConsentStatus,
        clinicalConsentStatus: 'withdrawn' satisfies ConsentStatus,
        clinicalConsentGranted: false,
      });
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
      throw err;
    } finally {
      setLoading(false);
    }
  }, [appointmentId, client, updateMetadata]);

  return {
    platformStatus,
    clinicalStatus,
    platformVersion: consentText.platform.version,
    clinicalVersion: consentText.clinical.version,
    loading,
    error,
    grantPlatformConsent: () => grantConsent('platform'),
    grantClinicalConsent: () => grantConsent('clinical'),
    withdrawConsent,
  };
}

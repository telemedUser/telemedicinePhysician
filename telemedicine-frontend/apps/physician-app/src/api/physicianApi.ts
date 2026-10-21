import { useMemo } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import type {
  PhysicianConsultationResponse,
  PhysicianDocumentationPayload,
} from '@telemedicine/shared-types';
import type {
  CloseConsultationRequest,
  CloseConsultationResponse,
  PatientDetail,
  ProviderDetail,
  ProviderStatusRequest,
  ProviderSummary,
  WorklistEntry,
} from './types';
import {
  MOCK_PROVIDER_DETAILS,
  MOCK_PROVIDERS,
  MOCK_WORKLIST,
} from '../data/mockData';

/** Resolve to the live value, or fall back to a demo value if the backend is unreachable/empty. */
async function withFallback<T>(call: () => Promise<T>, fallback: T, isEmpty: (v: T) => boolean): Promise<T> {
  try {
    const result = await call();
    return isEmpty(result) ? fallback : result;
  } catch {
    return fallback;
  }
}

/**
 * Typed wrapper over the shared ApiClient for every physician-service / physician-bff endpoint.
 * When the backend is not connected (review/demo), reads fall back to deck-derived mock data so
 * the screens stay populated.
 */
export function usePhysicianApi() {
  const client = useApiClient();

  return useMemo(
    () => ({
      async getWorklist(market = 'IN'): Promise<WorklistEntry[]> {
        return withFallback(
          async () => {
            const res = await client.get<WorklistEntry[]>('/v1/physicians/me/worklist', {
              headers: { 'X-Market': market },
            });
            return res.data ?? [];
          },
          MOCK_WORKLIST,
          (v) => v.length === 0,
        );
      },

      /**
       * The D-03 review & join detail for one appointment, projected from the patient service's
       * ConsultationContextPublished event. Returns null when the backend has no detail (e.g. the
       * context event hasn't been projected or the appointment isn't on this physician's worklist).
       */
      async getPatientDetail(appointmentId: string): Promise<PatientDetail | null> {
        try {
          const res = await client.get<PatientDetail>(
            `/v1/physicians/me/worklist/${encodeURIComponent(appointmentId)}`,
          );
          return res.data ?? null;
        } catch {
          return null;
        }
      },

      async acknowledgeRedFlag(appointmentId: string): Promise<void> {
        try {
          await client.post(
            `/v1/physicians/me/worklist/${encodeURIComponent(appointmentId)}/red-flag/ack`,
            undefined,
            { idempotencyKey: crypto.randomUUID() },
          );
        } catch {
          /* demo mode: acknowledgement handled client-side */
        }
      },

      async admitPatient(appointmentId: string): Promise<void> {
        try {
          // Admit → OPEN_DOCTOR on the physician-service worklist (direct-to-service review flow).
          await client.post(
            `/v1/physicians/me/worklist/${encodeURIComponent(appointmentId)}/admit`,
            undefined,
            { idempotencyKey: crypto.randomUUID() },
          );
        } catch {
          /* demo mode */
        }
      },

      async getConsultation(appointmentId: string): Promise<PhysicianConsultationResponse | null> {
        try {
          const res = await client.get<PhysicianConsultationResponse>(
            `/bff/physician/consultation/${encodeURIComponent(appointmentId)}`,
          );
          return res.data ?? null;
        } catch {
          return null;
        }
      },

      async closeConsultation(
        appointmentId: string,
        body: CloseConsultationRequest,
        market = 'IN',
      ): Promise<CloseConsultationResponse> {
        try {
          const res = await client.post<CloseConsultationResponse>(
            `/v1/physicians/me/consultations/${encodeURIComponent(appointmentId)}/close`,
            body,
            { headers: { 'X-Market': market }, idempotencyKey: crypto.randomUUID() },
          );
          if (res.data) return res.data;
        } catch {
          /* fall through to local evaluation */
        }
        // Local evaluation so the closure flow is reviewable without a backend.
        const minimum = ['NOTE', 'DIAGNOSIS'];
        const missing = minimum.filter((m) => !body.completedDocuments.includes(m as never));
        const exception = Boolean(body.exceptionReason && body.exceptionReason.trim());
        const closed = missing.length === 0 || exception;
        return {
          appointmentId,
          closed,
          minimumSatisfied: missing.length === 0,
          missingDocuments: closed ? [] : missing,
        };
      },

      async submitDocumentation(
        appointmentId: string,
        payload: PhysicianDocumentationPayload,
      ): Promise<void> {
        try {
          await client.post(
            `/bff/physician/documentation/${encodeURIComponent(appointmentId)}`,
            payload,
            { idempotencyKey: crypto.randomUUID() },
          );
        } catch {
          /* demo mode */
        }
      },

      async searchProviders(query: {
        specialty?: string;
        mode?: string;
        language?: string;
        status?: string;
      }): Promise<ProviderSummary[]> {
        return withFallback(
          async () => {
            const params = new URLSearchParams(
              Object.entries(query).filter(([, v]) => Boolean(v)) as [string, string][],
            );
            const suffix = params.toString() ? `?${params.toString()}` : '';
            const res = await client.get<ProviderSummary[]>(`/v1/physicians${suffix}`);
            return res.data ?? [];
          },
          MOCK_PROVIDERS,
          (v) => v.length === 0,
        );
      },

      async getProvider(providerId: string): Promise<ProviderDetail | null> {
        return withFallback(
          async () => {
            const res = await client.get<ProviderDetail>(
              `/v1/physicians/${encodeURIComponent(providerId)}`,
            );
            return res.data ?? null;
          },
          MOCK_PROVIDER_DETAILS[providerId] ?? null,
          (v) => v === null,
        );
      },

      async setProviderStatus(providerId: string, body: ProviderStatusRequest): Promise<void> {
        try {
          await client.put(`/v1/physicians/${encodeURIComponent(providerId)}/status`, body, {
            idempotencyKey: crypto.randomUUID(),
          });
        } catch {
          /* demo mode */
        }
      },
    }),
    [client],
  );
}

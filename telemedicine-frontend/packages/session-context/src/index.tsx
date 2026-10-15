import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { AppointmentContext, JourneyStatus, JourneyStep, PatientJourneyResponse } from '@telemedicine/shared-types';
import { hasReachedStatus } from '@telemedicine/shared-types';
import { useApiClient } from '@telemedicine/api-client';

const STORAGE_KEY = 'telemedicine.session';

export interface SessionState {
  sessionId: string | null;
  appointmentId: string | null;
  journeyStep: JourneyStep;
  journeyStatus: JourneyStatus;
  appointment: AppointmentContext | null;
  metadata: Record<string, unknown>;
}

export interface SessionContextValue {
  state: SessionState;
  setJourneyStep: (step: JourneyStep) => void;
  setJourneyStatus: (status: JourneyStatus) => void;
  setSessionId: (id: string | null) => void;
  setAppointmentId: (id: string | null) => void;
  updateMetadata: (patch: Record<string, unknown>) => void;
  hydrateFromBff: (appointmentId: string) => Promise<void>;
  reset: () => void;
}

const initialState: SessionState = {
  sessionId: null,
  appointmentId: null,
  journeyStep: 'consent',
  journeyStatus: 'LINK_GENERATED',
  appointment: null,
  metadata: {},
};

const SessionContext = createContext<SessionContextValue | null>(null);

function loadPersistedState(): SessionState {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (raw) return { ...initialState, ...JSON.parse(raw) };
  } catch {
    /* ignore */
  }
  return initialState;
}

function persistState(state: SessionState): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    /* ignore */
  }
}

function mapBffToState(data: PatientJourneyResponse): Partial<SessionState> {
  let journeyStep: JourneyStep = 'consent';
  if (data.survey?.status === 'OK') journeyStep = 'survey';
  else if (data.documents?.status === 'OK') journeyStep = 'documents';
  else if (data.video?.status === 'OK') journeyStep = 'consultation';
  else if (data.waitingRoom?.status === 'OK') journeyStep = 'waiting-room';
  else if (data.identity?.status === 'OK') journeyStep = 'identity';

  return {
    appointmentId: data.appointmentId,
    journeyStep,
    journeyStatus: data.journeyStatus ?? 'LINK_GENERATED',
    appointment: {
      appointmentId: data.appointmentId,
      status: data.journeyStatus ?? 'LINK_GENERATED',
    },
    metadata: {
      consent: data.consent,
      identity: data.identity,
      waitingRoom: data.waitingRoom,
      video: data.video,
      documents: data.documents,
      survey: data.survey,
    },
  };
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SessionState>(loadPersistedState);
  const client = useApiClient();

  useEffect(() => {
    persistState(state);
  }, [state]);

  const setJourneyStep = useCallback((journeyStep: JourneyStep) => {
    setState((s) => ({ ...s, journeyStep }));
    window.dispatchEvent(new CustomEvent('journey:stepChanged', { detail: { journeyStep } }));
  }, []);

  const setJourneyStatus = useCallback((journeyStatus: JourneyStatus) => {
    setState((s) => ({
      ...s,
      journeyStatus,
      appointment: s.appointment ? { ...s.appointment, status: journeyStatus } : null,
    }));
    window.dispatchEvent(new CustomEvent('journey:statusChanged', { detail: { journeyStatus } }));
  }, []);

  const hydrateFromBff = useCallback(
    async (appointmentId: string) => {
      const response = await client.get<PatientJourneyResponse>(`/bff/patient/journey/${appointmentId}`);
      if (response.data) {
        setState((s) => ({ ...s, ...mapBffToState(response.data!) }));
      }
    },
    [client],
  );

  const value = useMemo<SessionContextValue>(
    () => ({
      state,
      setJourneyStep,
      setJourneyStatus,
      setSessionId: (sessionId) => setState((s) => ({ ...s, sessionId })),
      setAppointmentId: (appointmentId) => setState((s) => ({ ...s, appointmentId })),
      updateMetadata: (patch) => setState((s) => ({ ...s, metadata: { ...s.metadata, ...patch } })),
      hydrateFromBff,
      reset: () => {
        sessionStorage.removeItem(STORAGE_KEY);
        setState(initialState);
      },
    }),
    [state, setJourneyStep, setJourneyStatus, hydrateFromBff],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession must be used within SessionProvider');
  return ctx;
}

export function useAppointment(): AppointmentContext | null {
  return useSession().state.appointment;
}

export function useJourneyGate(requiredStatus: JourneyStatus): boolean {
  const { state } = useSession();
  return hasReachedStatus(state.journeyStatus, requiredStatus);
}

export type { JourneyStep, JourneyStatus, SessionState };

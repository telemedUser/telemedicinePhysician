/**
 * Single source of truth for deriving a patient's worklist status from the REAL physician-service
 * worklist projection — never hard-coded. Both the Home dashboard (D-01) and the Worklist page
 * import this so the two screens are always in sync.
 *
 * The underlying data is fed by patient-side domain events (see WorklistProjectionListener):
 *   - PatientEnteredWaitingRoom  -> sets `enteredAt`, readiness = IN_PROGRESS   ("arrived, checks in progress")
 *   - DeviceTestCompleted        -> readiness = READY | NOT_READY, technicalStatus = OK | IMPAIRED
 *   - StatusChanged              -> consultationStatus = CHECKED_IN | OPEN_DOCTOR | STARTED | CLOSED
 * `waitingSeconds` is derived server-side from (now - enteredAt).
 *
 * Arrival is therefore defined by the data: a patient has arrived in the waiting room when
 * `enteredAt` is set. `readiness` then tells us whether their device checks are still running
 * (IN_PROGRESS) or passed (READY). No screen should re-implement this mapping.
 */
import type { ConsultationStatus, Readiness, TechnicalStatus } from './types';

/**
 * Minimal shape needed to derive a status. Both WorklistEntry (worklist/home) and PatientDetail
 * (D-03) satisfy it, so all three screens derive status from the one function.
 */
export interface StatusSource {
  enteredAt?: string | null;
  readiness: Readiness;
  consultationStatus: ConsultationStatus;
  technicalStatus: TechnicalStatus;
  waitingSeconds?: number;
}

/** Stable status keys, ordered roughly by where the patient is in the journey. */
export type WorklistStatusKey =
  | 'COMPLETED'
  | 'IN_CONSULTATION'
  | 'TECH_ISSUE'
  | 'READY'
  | 'IN_WAITING_ROOM'
  | 'NOT_ARRIVED';

export interface WorklistStatus {
  key: WorklistStatusKey;
  /** Short label for the status pill, e.g. "In waiting room". */
  label: string;
  /** Muted sub-line beneath the pill, e.g. "Arrived 16:31 · checks in progress". May be empty. */
  detail: string;
  /** Material symbol name for the pill icon. */
  icon: string;
  /** Pill colour class used by the worklist table (maps to theme.css `.wl-chip-*`). */
  chip: 'green' | 'blue' | 'amber' | 'red' | 'neutral';
  /** True once the patient has physically arrived in the waiting room (enteredAt is set). */
  arrived: boolean;
  /** True when the physician can admit/join now. */
  joinable: boolean;
}

function clock(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? ''
    : d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

function waitingLabel(seconds?: number): string {
  const mins = Math.max(0, Math.round((seconds ?? 0) / 60));
  return mins < 1 ? 'Waiting <1 min' : `Waiting ${mins} min`;
}

/**
 * Derive the display status for one worklist entry from its real fields. Precedence follows the
 * patient journey: closed → in-consultation → technical issue → arrived (ready / checks in
 * progress) → not arrived.
 */
export function deriveWorklistStatus(entry: StatusSource): WorklistStatus {
  const arrived = Boolean(entry.enteredAt);
  const arrivedAt = clock(entry.enteredAt ?? undefined);

  // Closed consultation — documentation done.
  if (entry.consultationStatus === 'CLOSED') {
    return {
      key: 'COMPLETED',
      label: 'Completed',
      detail: 'Docs delivered',
      icon: 'task_alt',
      chip: 'green',
      arrived,
      joinable: false,
    };
  }

  // The doctor has opened the room / the call is live.
  if (entry.consultationStatus === 'OPEN_DOCTOR' || entry.consultationStatus === 'STARTED') {
    return {
      key: 'IN_CONSULTATION',
      label: 'In consultation',
      detail: arrivedAt ? `Since ${arrivedAt}` : '',
      icon: 'videocam',
      chip: 'blue',
      arrived,
      joinable: false,
    };
  }

  // Arrived but the device/connection check failed — surfaced from DeviceTestCompleted.
  if (arrived && entry.technicalStatus === 'IMPAIRED') {
    return {
      key: 'TECH_ISSUE',
      label: 'Device issue',
      detail: arrivedAt ? `Arrived ${arrivedAt} · device check needed` : 'Device check needed',
      icon: 'wifi_off',
      chip: 'red',
      arrived,
      joinable: false,
    };
  }

  // Arrived and ready — device checks passed; the doctor can admit.
  if (arrived && entry.readiness === 'READY') {
    return {
      key: 'READY',
      label: 'Ready',
      detail: arrivedAt ? `Arrived ${arrivedAt} · ${waitingLabel(entry.waitingSeconds)}` : waitingLabel(entry.waitingSeconds),
      icon: 'check_circle',
      chip: 'green',
      arrived,
      joinable: true,
    };
  }

  // Arrived, checks still running (PatientEnteredWaitingRoom set enteredAt, readiness IN_PROGRESS).
  if (arrived) {
    return {
      key: 'IN_WAITING_ROOM',
      label: 'In waiting room',
      detail: arrivedAt ? `Arrived ${arrivedAt} · checks in progress` : 'Checks in progress',
      icon: 'hourglass_top',
      chip: 'amber',
      arrived,
      joinable: false,
    };
  }

  // No enteredAt yet — the patient has not joined the waiting room.
  return {
    key: 'NOT_ARRIVED',
    label: 'Not arrived',
    detail: '',
    icon: 'schedule',
    chip: 'neutral',
    arrived: false,
    joinable: false,
  };
}

/** True when the consultation is finished (shared with worklist tab counts). */
export function isCompleted(entry: StatusSource): boolean {
  return entry.consultationStatus === 'CLOSED';
}

/** True when the patient has arrived in the waiting room (data-driven: enteredAt is set). */
export function hasArrived(entry: StatusSource): boolean {
  return Boolean(entry.enteredAt);
}

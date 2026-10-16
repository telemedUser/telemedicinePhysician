/**
 * Lightweight physician session for local/dev auth. Holds the signed-in physician's id, name and
 * email. The id (e.g. "doc-khanna") is sent on every API request as the X-Physician-Id header, which
 * the physician-service dev security chain turns into the authenticated principal — so the worklist
 * is scoped to the signed-in doctor. Persisted to localStorage so a page refresh keeps the session.
 *
 * This mirrors the patient app's thin in-app auth pattern; it is not a replacement for the Keycloak
 * JWT flow used in a real environment.
 */
export interface PhysicianSession {
  physicianId: string;
  displayName: string;
  email: string;
}

const KEY = 'cc.physician.session';

let current: PhysicianSession | null = load();

function load(): PhysicianSession | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as PhysicianSession) : null;
  } catch {
    return null;
  }
}

export function getPhysicianSession(): PhysicianSession | null {
  return current;
}

export function setPhysicianSession(session: PhysicianSession): void {
  current = session;
  try {
    localStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    /* storage unavailable; keep in memory only */
  }
}

export function clearPhysicianSession(): void {
  current = null;
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}

/** The id sent as X-Physician-Id; null when signed out. */
export function currentPhysicianId(): string | null {
  return current?.physicianId ?? null;
}

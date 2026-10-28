/**
 * Lightweight physician session for local/dev auth. Holds the signed-in physician's id, name and
 * email. The id (e.g. "doc-khanna") is sent on every API request as the X-Physician-Id header, which
 * the physician-service dev security chain turns into the authenticated principal — so the worklist
 * is scoped to the signed-in doctor.
 *
 * Session-scoped by design: it is held in memory only (not persisted), so a fresh page load always
 * returns to the physician login screen and requires re-sign-in. Navigation within the open tab
 * stays signed in. This mirrors the patient app pattern; it is not a replacement for the Keycloak
 * JWT flow used in a real environment.
 */
export interface PhysicianSession {
  physicianId: string;
  displayName: string;
  email: string;
}

// In-memory only. Never restored at startup → a reload shows the login screen again.
let current: PhysicianSession | null = null;

export function getPhysicianSession(): PhysicianSession | null {
  return current;
}

export function setPhysicianSession(session: PhysicianSession): void {
  current = session;
}

export function clearPhysicianSession(): void {
  current = null;
}

/** The id sent as X-Physician-Id; null when signed out. */
export function currentPhysicianId(): string | null {
  return current?.physicianId ?? null;
}

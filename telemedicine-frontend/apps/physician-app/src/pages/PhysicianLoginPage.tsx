import { useState, type FormEvent } from 'react';
import { BpButton, Icon } from '../ui/primitives';
import { setPhysicianSession } from '../auth/physicianSession';

interface SignInResponse {
  success: boolean;
  data?: { physicianId: string; displayName: string; email: string };
}

/**
 * Physician sign-in (dev). Authenticates against the physician-service and stores the signed-in
 * doctor's id, which scopes the worklist to that physician. Styled with the shared Connected Care
 * design language.
 */
export function PhysicianLoginPage({ onSignedIn }: { onSignedIn: () => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(false);
    try {
      const res = await fetch('/api/auth/physician-sign-in', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) {
        setError(true);
        return;
      }
      const body = (await res.json()) as SignInResponse;
      if (!body.data) {
        setError(true);
        return;
      }
      setPhysicianSession(body.data);
      onSignedIn();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: 'var(--bp-paper)',
        padding: 20,
      }}
    >
      <div style={{ width: '100%', maxWidth: 400 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 20, justifyContent: 'center' }}>
          <span
            style={{
              width: 34,
              height: 34,
              borderRadius: 8,
              background: 'var(--bp-blue)',
              display: 'grid',
              placeItems: 'center',
            }}
          >
            <Icon name="health_and_safety" size={20} color="#fff" fill />
          </span>
          <strong style={{ fontSize: 18, color: 'var(--bp-navy)' }}>Connected Care</strong>
        </div>

        <div className="pg-infocard" style={{ background: 'var(--bp-surface)', padding: 24 }}>
          <h1 style={{ margin: '0 0 4px', fontSize: 20, color: 'var(--bp-navy)' }}>Physician sign in</h1>
          <p style={{ margin: '0 0 18px', color: 'var(--bp-muted)', fontSize: 14 }}>
            Sign in to see the patients booked with you.
          </p>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {error && (
              <div
                role="alert"
                style={{
                  background: 'var(--bp-danger-bg)',
                  color: 'var(--bp-danger)',
                  borderRadius: 8,
                  padding: '10px 12px',
                  fontSize: 13,
                  fontWeight: 600,
                }}
              >
                Email or password doesn't match.
              </div>
            )}
            <label style={{ display: 'block' }}>
              <span style={{ display: 'block', marginBottom: 6, fontSize: 13, fontWeight: 600, color: 'var(--bp-text-2)' }}>
                Email
              </span>
              <input
                type="email"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                style={inputStyle}
              />
            </label>
            <label style={{ display: 'block' }}>
              <span style={{ display: 'block', marginBottom: 6, fontSize: 13, fontWeight: 600, color: 'var(--bp-text-2)' }}>
                Password
              </span>
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                style={inputStyle}
              />
            </label>
            <BpButton type="submit" icon="login" loading={busy} style={{ width: '100%', marginTop: 4 }}>
              Sign in
            </BpButton>
          </form>

          <div
            style={{
              marginTop: 16,
              paddingTop: 14,
              borderTop: '1px solid var(--bp-border-2)',
              fontSize: 12,
              color: 'var(--bp-muted)',
              lineHeight: 1.6,
            }}
          >
            <strong style={{ color: 'var(--bp-text-2)' }}>Demo accounts</strong> (password{' '}
            <code>doctor123</code>):
            <br />
            arjun.khanna@connectedcare.in · meera.nair@connectedcare.in
            <br />
            vikram.reddy@connectedcare.in · lakshmi.iyer@connectedcare.in
          </div>
        </div>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px 12px',
  borderRadius: 8,
  border: '1px solid var(--bp-border)',
  fontSize: 14,
  color: 'var(--bp-text)',
  background: 'var(--bp-surface)',
  outline: 'none',
};

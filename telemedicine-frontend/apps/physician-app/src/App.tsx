import { useEffect, useState } from 'react';
import { useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { RequireAuth, RequireRole } from '@telemedicine/auth';
import { AppProviders } from './components/AppProviders';
import { Avatar, Icon } from './ui/primitives';
import { CURRENT_PHYSICIAN } from './data/mockData';
import { WorklistPage } from './pages/WorklistPage';
import { PatientContextPage } from './pages/PatientContextPage';
import { ConsultationWorkspacePage } from './pages/ConsultationWorkspacePage';
import { DocumentationPage } from './pages/DocumentationPage';
import { ReviewPage } from './pages/ReviewPage';
import { SendPage } from './pages/SendPage';
import { ClosePage } from './pages/ClosePage';
import { WithdrawnPage } from './pages/WithdrawnPage';
import { PreviousPatientsPage } from './pages/PreviousPatientsPage';
import { ProvidersPage } from './pages/ProvidersPage';
import { ProviderDetailPage } from './pages/ProviderDetailPage';

const NAV = [
  { to: '/worklist', label: 'Worklist', icon: 'list_alt' },
  { to: '/previous', label: 'Previous patients', icon: 'history' },
  { to: '/providers', label: 'Physicians', icon: 'stethoscope' },
];

function TopBar({ clock }: { clock: string }) {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const runSearch = () => {
    const q = search.trim();
    navigate(q ? `/previous?q=${encodeURIComponent(q)}` : '/previous');
  };

  return (
    <header
      style={{
        height: 56,
        background: 'var(--bp-navy)',
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        padding: '0 18px',
        flex: 'none',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <span
          style={{
            width: 28,
            height: 28,
            borderRadius: 7,
            background: 'var(--bp-blue)',
            display: 'grid',
            placeItems: 'center',
          }}
        >
          <Icon name="health_and_safety" size={18} color="#fff" fill />
        </span>
        <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.15 }}>
          <strong style={{ fontSize: 14, letterSpacing: '.01em' }}>Connected Care</strong>
          <span style={{ fontSize: 11, color: 'var(--bp-on-dark-muted)' }}>
            HIS · {CURRENT_PHYSICIAN.clinic}
          </span>
        </div>
      </div>

      <div
        style={{
          flex: '1 1 220px',
          maxWidth: 440,
          height: 32,
          borderRadius: 8,
          background: 'var(--bp-slate)',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          padding: '0 12px',
          color: 'var(--bp-on-dark-muted)',
        }}
      >
        <Icon name="search" size={18} />
        <input
          aria-label="Search patients"
          placeholder="Search patient, ABHA ID, diagnosis…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') runSearch();
          }}
          style={{
            flex: 1,
            background: 'transparent',
            border: 'none',
            outline: 'none',
            color: '#fff',
            fontSize: 13,
          }}
        />
        {search && (
          <button
            type="button"
            aria-label="Search"
            onClick={runSearch}
            style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', display: 'flex' }}
          >
            <Icon name="arrow_forward" size={16} />
          </button>
        )}
      </div>

      <div style={{ flex: 1 }} />

      <span style={{ font: '600 13px ui-monospace, Menlo, monospace', color: 'var(--bp-on-dark)' }}>
        {clock}
      </span>
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          padding: '4px 12px 4px 4px',
          borderRadius: 999,
          background: 'var(--bp-slate)',
          fontSize: 13,
        }}
      >
        <Avatar initials={CURRENT_PHYSICIAN.initials} size={24} />
        {CURRENT_PHYSICIAN.name}
      </span>
    </header>
  );
}

function SideNav() {
  const location = useLocation();
  const isActive = (to: string) => location.pathname.startsWith(to);
  return (
    <nav
      aria-label="Navigation"
      style={{
        width: 232,
        flex: 'none',
        background: 'var(--bp-nav)',
        borderRight: '1px solid var(--bp-border)',
        padding: '14px 10px',
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
      }}
    >
      <div style={{ padding: '0 10px 8px' }}>
        <span
          style={{
            fontSize: 11,
            fontWeight: 600,
            color: 'var(--bp-muted)',
            textTransform: 'uppercase',
            letterSpacing: '.06em',
          }}
        >
          Virtual clinic
        </span>
      </div>
      {NAV.map((item) => {
        const active = isActive(item.to);
        return (
          <Link
            key={item.to}
            to={item.to}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 11,
              padding: '9px 12px',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: active ? 700 : 500,
              color: active ? 'var(--bp-navy)' : 'var(--bp-text-2)',
              background: active ? '#fff' : 'transparent',
              boxShadow: active ? 'var(--bp-shadow)' : 'none',
            }}
          >
            <Icon name={item.icon} size={20} color={active ? 'var(--bp-blue)' : 'var(--bp-muted)'} fill={active} />
            {item.label}
          </Link>
        );
      })}
      <div style={{ flex: 1 }} />
      <div
        style={{
          padding: 12,
          borderRadius: 10,
          background: 'var(--bp-blue-tint)',
          color: 'var(--bp-blue-dark)',
          fontSize: 12,
          lineHeight: 1.5,
        }}
      >
        <Icon name="verified_user" size={16} /> HIS session · MFA satisfied
      </div>
    </nav>
  );
}

function AuthedShell() {
  const [clock, setClock] = useState('');
  useEffect(() => {
    const tick = () =>
      setClock(
        new Date().toLocaleString('en-US', {
          weekday: 'short',
          hour: '2-digit',
          minute: '2-digit',
        }),
      );
    tick();
    const id = window.setInterval(tick, 15_000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <TopBar clock={clock} />
      <div style={{ display: 'flex', flex: 1, minHeight: 0 }}>
        <SideNav />
        <main style={{ flex: 1, overflow: 'auto', padding: '24px 28px' }}>
          <div style={{ maxWidth: 1040, margin: '0 auto', animation: 'bpIn .25s ease' }}>
            <Routes>
              <Route path="/" element={<Navigate to="/worklist" replace />} />
              <Route path="/worklist" element={<WorklistPage />} />
              <Route path="/worklist/:appointmentId/context" element={<PatientContextPage />} />
              <Route path="/consultation/:appointmentId" element={<ConsultationWorkspacePage />} />
              <Route path="/consultation/:appointmentId/documentation" element={<DocumentationPage />} />
              <Route path="/consultation/:appointmentId/review" element={<ReviewPage />} />
              <Route path="/consultation/:appointmentId/send" element={<SendPage />} />
              <Route path="/consultation/:appointmentId/close" element={<ClosePage />} />
              <Route path="/consultation/:appointmentId/withdrawn" element={<WithdrawnPage />} />
              <Route path="/previous" element={<PreviousPatientsPage />} />
              <Route path="/providers" element={<ProvidersPage />} />
              <Route path="/providers/:providerId" element={<ProviderDetailPage />} />
              <Route path="*" element={<Navigate to="/worklist" replace />} />
            </Routes>
          </div>
        </main>
      </div>
    </div>
  );
}

export function App() {
  return (
    <AppProviders>
      <RequireAuth>
        <RequireRole roles={['PHYSICIAN']}>
          <AuthedShell />
        </RequireRole>
      </RequireAuth>
    </AppProviders>
  );
}

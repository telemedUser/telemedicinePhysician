import { useEffect, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import {
  DataTable,
  ErrorMessage,
  LoadingSpinner,
  PageLayout,
  StatusBadge,
} from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';

interface RealmRolesResponse {
  roles: string[];
}

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180';
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? 'telemedicine';

export default function App() {
  const { t } = useTranslation('adminAccess');
  const client = useApiClient();
  const [roles, setRoles] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    client
      .get<RealmRolesResponse>('/bff/admin/access/roles')
      .then((res) => setRoles(res.data?.roles ?? []))
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [client]);

  const keycloakAdminUrl = `${KEYCLOAK_URL}/admin/${KEYCLOAK_REALM}/console/#/${KEYCLOAK_REALM}/roles`;

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      <p>{t('readOnly')}</p>
      <p>
        <a href={keycloakAdminUrl} target="_blank" rel="noopener noreferrer">
          {t('keycloakLink')}
        </a>
      </p>

      {error && <ErrorMessage message={error} />}
      {loading ? (
        <LoadingSpinner label={t('roles')} />
      ) : (
        <>
          <h3>{t('roles')}</h3>
          <DataTable
            columns={[{ key: 'role', header: t('roles') }]}
            data={roles.map((role) => ({ role }))}
          />
          <div style={{ marginTop: '1rem' }}>
            {roles.map((role) => (
              <StatusBadge key={role} variant="ok">
                {role}
              </StatusBadge>
            ))}
          </div>
        </>
      )}
    </PageLayout>
  );
}

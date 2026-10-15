import { useEffect, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import { ErrorMessage, LoadingSpinner } from '@telemedicine/design-system';
import { useTranslation } from '@telemedicine/i18n';
import type { ApiResponse } from '@telemedicine/shared-types';

export function HomePage() {
  const client = useApiClient();
  const { t } = useTranslation('common');
  const [javaMessage, setJavaMessage] = useState<string>(t('loading'));
  const [pythonMessage, setPythonMessage] = useState<string>(t('loading'));
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      client.get<{ message: string }>('/v1/template-java/hello'),
      client.get<{ message: string }>('/v1/template-python/hello'),
    ])
      .then(([javaRes, pythonRes]) => {
        setJavaMessage(javaRes.data?.message ?? 'No response');
        setPythonMessage(pythonRes.data?.message ?? 'No response');
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [client, t]);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div>
      <h2>{t('shellTitle')}</h2>
      <p>{t('shellDescription')}</p>
      <ul>
        <li>Java service: {javaMessage}</li>
        <li>Python service: {pythonMessage}</li>
      </ul>
    </div>
  );
}

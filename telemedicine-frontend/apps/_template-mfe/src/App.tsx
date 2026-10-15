import { useEffect, useState } from 'react';
import type { ApiResponse } from '@telemedicine/shared-types';
import { ErrorMessage } from '@telemedicine/shared-ui';

export default function App() {
  const [message, setMessage] = useState('Loading backend...');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/v1/template-python/hello')
      .then((response) => response.json())
      .then((body: ApiResponse<{ message: string }>) => {
        setMessage(body.data?.message ?? 'No message');
      })
      .catch((err: Error) => setError(err.message));
  }, []);

  if (error) {
    return <ErrorMessage message={error} />;
  }

  return (
    <div>
      <h2>Template Microfrontend</h2>
      <p>This remote is loaded via Module Federation into the shell container.</p>
      <p>Backend says: {message}</p>
    </div>
  );
}

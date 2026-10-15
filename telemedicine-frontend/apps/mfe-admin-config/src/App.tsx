import { useCallback, useEffect, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import {
  Button,
  Checkbox,
  ConfirmDialog,
  ErrorMessage,
  FormField,
  LoadingSpinner,
  PageLayout,
  Select,
} from '@telemedicine/design-system';
import { ConfigKeys, mfeConfigKey } from '@telemedicine/shared-types';
import { useTranslation } from '@telemedicine/i18n';

interface AdminConfigView {
  videoProvider: string;
  chatProvider: string;
  telemetryEnabled: boolean;
  notificationChannels: Record<string, boolean>;
  mfeRemotes: Record<string, boolean>;
  telemetryRulesJson?: string;
}

const VIDEO_PROVIDERS = ['simulator', 'vonage', 'acs'] as const;
const CHAT_PROVIDERS = ['simulator', 'vonage', 'acs'] as const;

export default function App() {
  const { t } = useTranslation('adminConfig');
  const client = useApiClient();
  const [config, setConfig] = useState<AdminConfigView | null>(null);
  const [draft, setDraft] = useState<AdminConfigView | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingKey, setPendingKey] = useState<string | null>(null);
  const [pendingValue, setPendingValue] = useState<unknown>(null);

  const loadConfig = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get<AdminConfigView>('/bff/admin/config');
      const data = res.data ?? null;
      setConfig(data);
      setDraft(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load configuration');
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  const requestSave = (key: string, value: unknown) => {
    setPendingKey(key);
    setPendingValue(value);
  };

  const confirmSave = async () => {
    if (!pendingKey || draft === null) return;
    setSaving(true);
    setError(null);
    try {
      await client.put(`/bff/admin/config/${encodeURIComponent(pendingKey)}`, pendingValue);
      setConfig(draft);
      setPendingKey(null);
      setPendingValue(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <PageLayout>
        <LoadingSpinner label={t('title')} />
      </PageLayout>
    );
  }

  if (!draft) {
    return (
      <PageLayout>
        <ErrorMessage message={error ?? 'Configuration unavailable'} />
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <h2>{t('title')}</h2>
      {error && <ErrorMessage message={error} />}

      <section aria-labelledby="providers-heading">
        <h3 id="providers-heading">{t('videoProvider')}</h3>
        <FormField label={t('videoProvider')}>
          <Select
            value={draft.videoProvider}
            onChange={(e) => {
              const value = e.target.value;
              setDraft((prev) => (prev ? { ...prev, videoProvider: value } : prev));
              requestSave(ConfigKeys.VIDEO_PROVIDER, value);
            }}
          >
            {VIDEO_PROVIDERS.map((provider) => (
              <option key={provider} value={provider}>
                {provider}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label={t('chatProvider')}>
          <Select
            value={draft.chatProvider}
            onChange={(e) => {
              const value = e.target.value;
              setDraft((prev) => (prev ? { ...prev, chatProvider: value } : prev));
              requestSave(ConfigKeys.CHAT_PROVIDER, value);
            }}
          >
            {CHAT_PROVIDERS.map((provider) => (
              <option key={provider} value={provider}>
                {provider}
              </option>
            ))}
          </Select>
        </FormField>
      </section>

      <section aria-labelledby="telemetry-heading">
        <h3 id="telemetry-heading">{t('telemetry')}</h3>
        <Checkbox
          label={t('telemetry')}
          checked={draft.telemetryEnabled}
          onChange={(e) => {
            const value = e.target.checked;
            setDraft((prev) => (prev ? { ...prev, telemetryEnabled: value } : prev));
            requestSave(ConfigKeys.TELEMETRY_ENABLED, value);
          }}
        />
        {draft.telemetryRulesJson && (
          <p style={{ fontSize: '0.875rem', color: 'var(--tm-color-text-muted)' }}>{draft.telemetryRulesJson}</p>
        )}
      </section>

      <section aria-labelledby="notifications-heading">
        <h3 id="notifications-heading">{t('notifications')}</h3>
        {Object.entries(draft.notificationChannels).map(([channel, enabled]) => (
          <Checkbox
            key={channel}
            label={channel}
            checked={enabled}
            onChange={(e) => {
              const value = e.target.checked;
              setDraft((prev) =>
                prev
                  ? {
                      ...prev,
                      notificationChannels: { ...prev.notificationChannels, [channel]: value },
                    }
                  : prev,
              );
              requestSave(`notification.${channel}.enabled`, value);
            }}
          />
        ))}
      </section>

      <section aria-labelledby="mfe-heading">
        <h3 id="mfe-heading">{t('mfeEnablement')}</h3>
        {Object.entries(draft.mfeRemotes).map(([remote, enabled]) => (
          <Checkbox
            key={remote}
            label={remote}
            checked={enabled}
            onChange={(e) => {
              const value = e.target.checked;
              setDraft((prev) =>
                prev ? { ...prev, mfeRemotes: { ...prev.mfeRemotes, [remote]: value } } : prev,
              );
              requestSave(mfeConfigKey(remote), value);
            }}
          />
        ))}
      </section>

      {config && (
        <p style={{ fontSize: '0.875rem', color: 'var(--tm-color-text-muted)' }}>
          {t('auditTrail')}: {config.videoProvider} / {config.chatProvider}
        </p>
      )}

      <Button disabled={saving} onClick={() => void loadConfig()}>
        {t('save')}
      </Button>

      <ConfirmDialog
        open={pendingKey !== null}
        title={t('confirmTitle')}
        message={t('confirmBody')}
        confirmLabel={t('save')}
        onConfirm={() => void confirmSave()}
        onCancel={() => {
          setDraft(config);
          setPendingKey(null);
          setPendingValue(null);
        }}
      />
    </PageLayout>
  );
}

import { useCallback, useState } from 'react';
import { useApiClient } from '@telemedicine/api-client';
import { useSession } from '@telemedicine/session-context';
import {
  enumerateDevices,
  loadDevicePreferences,
  saveDevicePreferences,
} from './DeviceSelector';
import type { PreflightReport } from './preflight';
import { runPreflightChecks, submitDeviceTestResults } from './preflight';

export function useDeviceTest(appointmentId?: string) {
  const client = useApiClient();
  const { state } = useSession();
  const resolvedAppointmentId = appointmentId ?? state.appointmentId ?? undefined;
  const prefs = loadDevicePreferences();

  const [report, setReport] = useState<PreflightReport | null>(null);
  const [devices, setDevices] = useState<MediaDeviceInfo[]>([]);
  const [cameraDeviceId, setCameraDeviceId] = useState(prefs.cameraDeviceId);
  const [microphoneDeviceId, setMicrophoneDeviceId] = useState(prefs.microphoneDeviceId);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const refreshDevices = useCallback(async () => {
    const list = await enumerateDevices();
    setDevices(list);
  }, []);

  const runTests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await runPreflightChecks({ cameraDeviceId, microphoneDeviceId });
      setReport(result);
      return result;
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
      throw err;
    } finally {
      setLoading(false);
    }
  }, [cameraDeviceId, microphoneDeviceId]);

  const submitResults = useCallback(
    async (result: PreflightReport = report!) => {
      if (!resolvedAppointmentId) throw new Error('No appointment ID');
      setLoading(true);
      try {
        await client.post(`/v1/waiting-room/device-test/${resolvedAppointmentId}`, result, {
          idempotencyKey: crypto.randomUUID(),
        });
      } finally {
        setLoading(false);
      }
    },
    [client, resolvedAppointmentId, report],
  );

  const selectCamera = useCallback((deviceId: string) => {
    setCameraDeviceId(deviceId);
    saveDevicePreferences({ ...loadDevicePreferences(), cameraDeviceId: deviceId });
  }, []);

  const selectMicrophone = useCallback((deviceId: string) => {
    setMicrophoneDeviceId(deviceId);
    saveDevicePreferences({ ...loadDevicePreferences(), microphoneDeviceId: deviceId });
  }, []);

  return {
    report,
    devices,
    cameraDeviceId,
    microphoneDeviceId,
    loading,
    error,
    refreshDevices,
    runTests,
    submitResults,
    selectCamera,
    selectMicrophone,
  };
}

export { submitDeviceTestResults };

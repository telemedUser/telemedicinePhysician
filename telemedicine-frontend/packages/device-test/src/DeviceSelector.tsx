import { useCallback, useEffect, useState } from 'react';
import type { MediaDeviceInfo } from './DeviceSelector';

const STORAGE_KEY = 'telemedicine.device-preferences';

export interface DevicePreferences {
  cameraDeviceId?: string;
  microphoneDeviceId?: string;
}

export function loadDevicePreferences(): DevicePreferences {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as DevicePreferences;
  } catch {
    return {};
  }
}

export function saveDevicePreferences(prefs: DevicePreferences): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
}

export interface DeviceSelectorProps {
  devices: MediaDeviceInfo[];
  selectedCameraId?: string;
  selectedMicrophoneId?: string;
  onCameraChange: (deviceId: string) => void;
  onMicrophoneChange: (deviceId: string) => void;
}

export function DeviceSelector({
  devices,
  selectedCameraId,
  selectedMicrophoneId,
  onCameraChange,
  onMicrophoneChange,
}: DeviceSelectorProps) {
  const cameras = devices.filter((d) => d.kind === 'videoinput');
  const microphones = devices.filter((d) => d.kind === 'audioinput');

  return (
    <div data-testid="device-selector" style={{ display: 'grid', gap: '0.75rem' }}>
      <label>
        Camera
        <select
          aria-label="Select camera"
          value={selectedCameraId ?? ''}
          onChange={(e) => onCameraChange(e.target.value)}
        >
          {cameras.map((d) => (
            <option key={d.deviceId} value={d.deviceId}>
              {d.label || `Camera ${d.deviceId.slice(0, 6)}`}
            </option>
          ))}
        </select>
      </label>
      <label>
        Microphone
        <select
          aria-label="Select microphone"
          value={selectedMicrophoneId ?? ''}
          onChange={(e) => onMicrophoneChange(e.target.value)}
        >
          {microphones.map((d) => (
            <option key={d.deviceId} value={d.deviceId}>
              {d.label || `Microphone ${d.deviceId.slice(0, 6)}`}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}

export type { MediaDeviceInfo };

export async function enumerateDevices(): Promise<MediaDeviceInfo[]> {
  if (!navigator.mediaDevices?.enumerateDevices) return [];
  return navigator.mediaDevices.enumerateDevices();
}

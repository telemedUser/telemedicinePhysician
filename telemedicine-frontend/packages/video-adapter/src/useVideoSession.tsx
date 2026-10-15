import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { NetworkQuality, VideoParticipant, VideoRole } from './types';
import { useVideoAdapter } from './createVideoAdapter';

export interface UseVideoSessionOptions {
  role?: VideoRole;
  autoJoin?: boolean;
}

export interface UseVideoSessionResult {
  sessionId: string | null;
  isConnected: boolean;
  isMuted: { audio: boolean; video: boolean };
  networkQuality: NetworkQuality;
  participants: VideoParticipant[];
  localStream: MediaStream | null;
  remoteStreams: MediaStream[];
  toggleAudio: () => Promise<void>;
  toggleVideo: () => Promise<void>;
  switchDevice: (kind: 'camera' | 'microphone', deviceId: string) => Promise<void>;
  join: () => Promise<void>;
  leave: () => Promise<void>;
  error: Error | null;
}

export function useVideoSession(
  appointmentId: string,
  options: UseVideoSessionOptions = {},
): UseVideoSessionResult {
  const { role = 'patient', autoJoin = true } = options;
  const adapter = useVideoAdapter();
  const adapterRef = useRef(adapter);
  adapterRef.current = adapter;

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isMuted, setIsMuted] = useState({ audio: false, video: false });
  const [networkQuality, setNetworkQuality] = useState<NetworkQuality>(5);
  const [participants, setParticipants] = useState<VideoParticipant[]>([]);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStreams] = useState<MediaStream[]>([]);
  const [error, setError] = useState<Error | null>(null);

  const join = useCallback(async () => {
    try {
      setError(null);
      const session = await adapterRef.current.createSession(appointmentId);
      setSessionId(session.sessionId);
      await adapterRef.current.joinSession(session.sessionId, role);
      setIsConnected(true);
      setNetworkQuality(adapterRef.current.getNetworkQuality());
      if (typeof navigator !== 'undefined' && navigator.mediaDevices) {
        try {
          const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
          setLocalStream(stream);
        } catch {
          /* simulator mode may run without real devices */
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
    }
  }, [appointmentId, role]);

  const leave = useCallback(async () => {
    await adapterRef.current.leaveSession();
    localStream?.getTracks().forEach((t) => t.stop());
    setLocalStream(null);
    setIsConnected(false);
    setParticipants([]);
    setSessionId(null);
  }, [localStream]);

  const toggleAudio = useCallback(async () => {
    const next = !isMuted.audio;
    await adapterRef.current.muteAudio(next);
    setIsMuted((prev) => ({ ...prev, audio: next }));
  }, [isMuted.audio]);

  const toggleVideo = useCallback(async () => {
    const next = !isMuted.video;
    await adapterRef.current.muteVideo(next);
    setIsMuted((prev) => ({ ...prev, video: next }));
  }, [isMuted.video]);

  const switchDevice = useCallback(async (kind: 'camera' | 'microphone', deviceId: string) => {
    if (kind === 'camera') {
      await adapterRef.current.switchCamera(deviceId);
    } else {
      await adapterRef.current.switchMicrophone(deviceId);
    }
  }, []);

  useEffect(() => {
    const unsubscribers = [
      adapter.onParticipantJoined((p) => setParticipants((prev) => [...prev, p])),
      adapter.onParticipantLeft((p) =>
        setParticipants((prev) => prev.filter((x) => x.id !== p.id)),
      ),
      adapter.onNetworkQualityChanged(setNetworkQuality),
      adapter.onDisconnected(() => setIsConnected(false)),
    ];
    return () => {
      unsubscribers.forEach((unsub) => unsub());
    };
  }, [adapter]);

  useEffect(() => {
    if (autoJoin && appointmentId) {
      void join();
    }
    return () => {
      adapterRef.current.destroy();
      localStream?.getTracks().forEach((t) => t.stop());
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appointmentId, autoJoin]);

  return useMemo(
    () => ({
      sessionId,
      isConnected,
      isMuted,
      networkQuality,
      participants,
      localStream,
      remoteStreams,
      toggleAudio,
      toggleVideo,
      switchDevice,
      join,
      leave,
      error,
    }),
    [
      sessionId,
      isConnected,
      isMuted,
      networkQuality,
      participants,
      localStream,
      remoteStreams,
      toggleAudio,
      toggleVideo,
      switchDevice,
      join,
      leave,
      error,
    ],
  );
}

import { useEffect, useRef, useState } from 'react';

export type CallRole = 'doctor' | 'patient';

export interface UseWebRtcCallOptions {
  roomId: string;
  role: CallRole;
  wsUrl: string;
  iceServers?: RTCIceServer[];
  onCallStateChange?: (connected: boolean) => void;
}

export interface WebRtcCallState {
  localRef: React.RefObject<HTMLVideoElement>;
  remoteRef: React.RefObject<HTMLVideoElement>;
  status: string;
  joined: boolean;
  connected: boolean;
  micOn: boolean;
  cameraOn: boolean;
  toggleMic: () => void;
  toggleCamera: () => void;
  endCall: () => void;
}

/**
 * One-to-one WebRTC call over the shared /ws signaling relay. Patient and physician join the same
 * roomId (the appointment reference) and establish a direct peer connection carrying real camera/mic
 * media. ICE servers (STUN + TURN) are passed in so the call connects across networks, not just LAN.
 *
 * The physician is the "doctor" (offerer). To be robust to join order, the answerer (patient)
 * replies to `peer-joined` with a `peer-here` ping, and the offerer creates the SDP offer when it
 * sees EITHER message — so the call connects whether the doctor or the patient joined first.
 */
export function useWebRtcCall(opts: UseWebRtcCallOptions): WebRtcCallState {
  const { roomId, role, wsUrl, iceServers = [], onCallStateChange } = opts;

  const localRef = useRef<HTMLVideoElement>(null);
  const remoteRef = useRef<HTMLVideoElement>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const pcRef = useRef<RTCPeerConnection | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const pendingCandidates = useRef<RTCIceCandidate[]>([]);
  const offerStarted = useRef(false);

  const [micOn, setMicOn] = useState(true);
  const [cameraOn, setCameraOn] = useState(true);
  const [status, setStatus] = useState('Connecting signaling…');
  const [joined, setJoined] = useState(false);
  const [connected, setConnected] = useState(false);

  const send = (obj: Record<string, unknown> & { type: string }) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ roomId, ...obj }));
    }
  };

  useEffect(() => {
    let alive = true;

    const makeOffer = async (pc: RTCPeerConnection) => {
      if (offerStarted.current || pc.remoteDescription) return;
      offerStarted.current = true;
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      send({ type: 'offer', sdp: offer });
    };

    const start = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
        if (!alive) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (localRef.current) localRef.current.srcObject = stream;

        const pc = new RTCPeerConnection({ iceServers });
        pcRef.current = pc;
        stream.getTracks().forEach((track) => pc.addTrack(track, stream));

        pc.ontrack = (e) => {
          if (remoteRef.current) remoteRef.current.srcObject = e.streams[0];
          setStatus('Media connected');
        };
        pc.onicecandidate = (e) => {
          if (e.candidate) send({ type: 'ice', candidate: e.candidate });
        };
        pc.onconnectionstatechange = () => {
          const isConnected = pc.connectionState === 'connected';
          setConnected(isConnected);
          setStatus(isConnected ? 'Connected' : `Call: ${pc.connectionState}`);
          onCallStateChange?.(isConnected);
        };

        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;
        ws.onopen = () => {
          send({ type: 'join', role });
          setJoined(true);
          setStatus('Waiting for the other participant…');
        };
        ws.onmessage = async (event) => {
          let msg: { type?: string; sdp?: RTCSessionDescriptionInit; candidate?: RTCIceCandidateInit };
          try {
            msg = JSON.parse(event.data);
          } catch {
            return;
          }
          if (msg.type === 'chat') return;
          try {
            if (msg.type === 'peer-joined') {
              // A peer is in the room. The answerer announces itself so the offerer can start even
              // if it joined first; the offerer kicks off the offer.
              if (role === 'patient') send({ type: 'peer-here', role });
              if (role === 'doctor') await makeOffer(pc);
            } else if (msg.type === 'peer-here') {
              if (role === 'doctor') await makeOffer(pc);
            } else if (msg.type === 'offer' && msg.sdp) {
              await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp));
              for (const c of pendingCandidates.current) await pc.addIceCandidate(c);
              pendingCandidates.current = [];
              const answer = await pc.createAnswer();
              await pc.setLocalDescription(answer);
              send({ type: 'answer', sdp: answer });
            } else if (msg.type === 'answer' && msg.sdp) {
              await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp));
              for (const c of pendingCandidates.current) await pc.addIceCandidate(c);
              pendingCandidates.current = [];
            } else if (msg.type === 'ice' && msg.candidate) {
              const c = new RTCIceCandidate(msg.candidate);
              if (pc.remoteDescription) await pc.addIceCandidate(c);
              else pendingCandidates.current.push(c);
            }
          } catch (err) {
            setStatus('Call setup error: ' + (err as Error).message);
          }
        };
        ws.onerror = () => setStatus('Signaling connection failed');
        ws.onclose = () => {
          if (alive && !connected) setStatus('Signaling disconnected');
        };
      } catch (err) {
        setStatus(
          `Camera/microphone unavailable: ${(err as Error).message}. Use HTTPS or localhost and allow permissions.`,
        );
      }
    };

    void start();

    return () => {
      alive = false;
      offerStarted.current = false;
      wsRef.current?.close();
      pcRef.current?.close();
      streamRef.current?.getTracks().forEach((t) => t.stop());
      onCallStateChange?.(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, role, wsUrl]);

  const toggle = (kind: 'audio' | 'video') => {
    const track = streamRef.current?.getTracks().find((t) => t.kind === kind);
    if (track) track.enabled = !track.enabled;
    if (kind === 'audio') setMicOn(Boolean(track?.enabled));
    else setCameraOn(Boolean(track?.enabled));
  };

  const endCall = () => {
    wsRef.current?.close();
    pcRef.current?.close();
    streamRef.current?.getTracks().forEach((t) => t.stop());
    setStatus('Call ended');
    setJoined(false);
    setConnected(false);
    onCallStateChange?.(false);
  };

  return {
    localRef,
    remoteRef,
    status,
    joined,
    connected,
    micOn,
    cameraOn,
    toggleMic: () => toggle('audio'),
    toggleCamera: () => toggle('video'),
    endCall,
  };
}

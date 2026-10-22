import { Icon } from '../ui/primitives';
import { useWebRtcCall, type CallRole } from './useWebRtcCall';

export interface VideoCallProps {
  /** Room id — the appointment reference. Patient and physician join the same room. */
  roomId: string;
  role: CallRole;
  wsUrl: string;
  iceServers?: RTCIceServer[];
  onCallStateChange?: (connected: boolean) => void;
  onEnded?: () => void;
}

/**
 * Self-contained one-to-one video call for the physician app. The physician joins as the "doctor"
 * (offerer) into the room keyed by the appointment reference; the patient app joins the same room
 * as "patient". Real camera/mic media flows peer-to-peer; ICE servers make it work across networks.
 */
export function VideoCall({ roomId, role, wsUrl, iceServers, onCallStateChange, onEnded }: VideoCallProps) {
  const call = useWebRtcCall({ roomId, role, wsUrl, iceServers, onCallStateChange });

  const end = () => {
    call.endCall();
    onEnded?.();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          position: 'relative',
          background: '#0b1726',
          borderRadius: 14,
          overflow: 'hidden',
          aspectRatio: '16 / 10',
          minHeight: 300,
        }}
      >
        <video
          ref={call.remoteRef}
          autoPlay
          playsInline
          style={{ width: '100%', height: '100%', objectFit: 'cover', background: '#0b1726' }}
        />
        {!call.connected && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'grid',
              placeItems: 'center',
              color: '#9fb3c8',
              font: "500 15px/1.4 'Figtree', system-ui, sans-serif",
              textAlign: 'center',
              padding: 16,
              pointerEvents: 'none',
            }}
          >
            {call.status}
          </div>
        )}
        <div
          style={{
            position: 'absolute',
            right: 12,
            bottom: 12,
            width: 150,
            aspectRatio: '4 / 3',
            borderRadius: 10,
            overflow: 'hidden',
            border: '2px solid rgba(255,255,255,.4)',
            background: '#09111c',
          }}
        >
          <video
            ref={call.localRef}
            autoPlay
            muted
            playsInline
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
        <button type="button" onClick={call.toggleMic} className="vc-btn" title="Toggle microphone" aria-pressed={!call.micOn}>
          <Icon name={call.micOn ? 'mic' : 'mic_off'} size={20} color={call.micOn ? 'var(--bp-navy)' : 'var(--bp-danger)'} />
          {call.micOn ? 'Mute' : 'Unmute'}
        </button>
        <button type="button" onClick={call.toggleCamera} className="vc-btn" title="Toggle camera" aria-pressed={!call.cameraOn}>
          <Icon name={call.cameraOn ? 'videocam' : 'videocam_off'} size={20} color={call.cameraOn ? 'var(--bp-navy)' : 'var(--bp-danger)'} />
          {call.cameraOn ? 'Camera' : 'Camera off'}
        </button>
        <button type="button" onClick={end} className="vc-btn vc-btn-hangup" title="End call">
          <Icon name="call_end" size={20} color="#fff" />
          End call
        </button>
      </div>

      <div style={{ textAlign: 'center', font: "500 13px 'Figtree', system-ui, sans-serif", color: 'var(--bp-muted)' }}>
        {call.connected ? 'Connected' : call.joined ? `Signaling connected · ${call.status}` : call.status}
      </div>
    </div>
  );
}

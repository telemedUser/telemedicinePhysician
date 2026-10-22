/**
 * Runtime config for the WebRTC video call: the signaling WebSocket URL and the ICE servers used
 * for NAT traversal. ICE is what makes the call work "on the fly" across different networks (not
 * just a shared LAN):
 *   - STUN lets each peer discover its public IP/port so two peers behind different NATs can try a
 *     direct path.
 *   - TURN is the relay fallback for symmetric NATs / restrictive firewalls where a direct path is
 *     impossible; without a TURN server, calls between such networks will fail. Provide one via env.
 *
 * All values are overridable via Vite env so a deployment can point at its own signaling host and
 * its own TURN server without code changes.
 */
export interface VideoRuntimeConfig {
  /** WebSocket signaling URL, e.g. ws://host:8130/ws */
  wsUrl: string;
  /** ICE servers (STUN + optional TURN) for peer connectivity across networks. */
  iceServers: RTCIceServer[];
}

function env(): Record<string, string> {
  return (import.meta as unknown as { env?: Record<string, string> }).env ?? {};
}

/** Public STUN servers (Google) — enough for most home/office NATs to find a direct path. */
const DEFAULT_STUN: RTCIceServer[] = [
  { urls: ['stun:stun.l.google.com:19302', 'stun:stun1.l.google.com:19302'] },
];

/**
 * Build the ICE server list. A TURN server (strongly recommended for real cross-network calls) is
 * added when VITE_TURN_URL is set, with optional VITE_TURN_USERNAME / VITE_TURN_CREDENTIAL.
 * VITE_TURN_URL may be a comma-separated list, e.g.
 *   turn:turn.example.com:3478,turns:turn.example.com:5349
 */
export function resolveIceServers(): RTCIceServer[] {
  const e = env();
  const servers: RTCIceServer[] = [...DEFAULT_STUN];

  // Allow fully replacing/extending STUN via env too.
  if (e.VITE_STUN_URL) {
    servers.length = 0;
    servers.push({ urls: e.VITE_STUN_URL.split(',').map((s) => s.trim()).filter(Boolean) });
  }

  if (e.VITE_TURN_URL) {
    const turn: RTCIceServer = {
      urls: e.VITE_TURN_URL.split(',').map((s) => s.trim()).filter(Boolean),
    };
    if (e.VITE_TURN_USERNAME) turn.username = e.VITE_TURN_USERNAME;
    if (e.VITE_TURN_CREDENTIAL) turn.credential = e.VITE_TURN_CREDENTIAL;
    servers.push(turn);
  }
  return servers;
}

/** Resolve the signaling WS URL. Defaults to the video-consultation-service on :8130. */
export function resolveVideoConfig(): VideoRuntimeConfig {
  const e = env();
  const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost';
  const wsUrl = e.VITE_VIDEO_WS_URL ?? `ws://${host}:8130/ws`;
  return { wsUrl, iceServers: resolveIceServers() };
}

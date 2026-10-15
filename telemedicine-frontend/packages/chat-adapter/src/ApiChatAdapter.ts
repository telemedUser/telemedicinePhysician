import type { ApiClient } from '@telemedicine/api-client';
import type { ChatMessage, ClinicalChatAdapter, PresenceStatus } from './types';

export class ApiChatAdapter implements ClinicalChatAdapter {
  private readonly client: ApiClient;
  private sessionId: string | null = null;
  private messages: ChatMessage[] = [];
  private pollTimer: number | null = null;
  private readonly pollIntervalMs: number;

  private messageListeners = new Set<(msg: ChatMessage) => void>();
  private typingListeners = new Set<(id: string, isTyping: boolean) => void>();
  private presenceListeners = new Set<(id: string, status: PresenceStatus) => void>();

  constructor(client: ApiClient, pollIntervalMs = 2000) {
    this.client = client;
    this.pollIntervalMs = pollIntervalMs;
  }

  async connect(sessionId: string): Promise<void> {
    this.sessionId = sessionId;
    const res = await this.client.get<ChatMessage[]>(
      `/v1/video/sessions/${sessionId}/chat`,
    );
    this.messages = res.data ?? [];
    this.startPolling();
    this.presenceListeners.forEach((cb) => cb('local', 'online'));
  }

  async sendMessage(text: string, clinical = false): Promise<ChatMessage> {
    if (!this.sessionId) throw new Error('Not connected');
    const res = await this.client.post<ChatMessage>(
      `/v1/video/sessions/${this.sessionId}/chat`,
      { body: text, clinical },
    );
    const message = res.data;
    this.messages.push(message);
    this.messageListeners.forEach((cb) => cb(message));
    return message;
  }

  getMessages(): ChatMessage[] {
    return [...this.messages];
  }

  onMessageReceived(callback: (msg: ChatMessage) => void): () => void {
    this.messageListeners.add(callback);
    return () => this.messageListeners.delete(callback);
  }

  onTypingIndicator(callback: (participantId: string, isTyping: boolean) => void): () => void {
    this.typingListeners.add(callback);
    return () => this.typingListeners.delete(callback);
  }

  onPresenceChange(callback: (participantId: string, status: PresenceStatus) => void): () => void {
    this.presenceListeners.add(callback);
    return () => this.presenceListeners.delete(callback);
  }

  setTyping(isTyping: boolean): void {
    if (!this.sessionId) return;
    void this.client.post(`/v1/video/sessions/${this.sessionId}/chat/typing`, { isTyping });
    this.typingListeners.forEach((cb) => cb('local', isTyping));
  }

  async disconnect(): Promise<void> {
    this.stopPolling();
    this.presenceListeners.forEach((cb) => cb('local', 'offline'));
    this.sessionId = null;
    this.messages = [];
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollTimer = window.setInterval(() => void this.poll(), this.pollIntervalMs);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      window.clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private async poll(): Promise<void> {
    if (!this.sessionId) return;
    try {
      const res = await this.client.get<ChatMessage[]>(
        `/v1/video/sessions/${this.sessionId}/chat`,
      );
      const incoming = res.data ?? [];
      const known = new Set(this.messages.map((m) => m.id));
      for (const msg of incoming) {
        if (!known.has(msg.id)) {
          this.messages.push(msg);
          this.messageListeners.forEach((cb) => cb(msg));
        }
      }
    } catch {
      /* polling errors are non-fatal */
    }
  }
}

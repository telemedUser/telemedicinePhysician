import type { ChatMessage, ClinicalChatAdapter, PresenceStatus } from './types';

/** In-memory chat with simulated physician echo responses. */
export class SimulatorChatAdapter implements ClinicalChatAdapter {
  private sessionId: string | null = null;
  private messages: ChatMessage[] = [];
  private typingTimer: number | null = null;

  private messageListeners = new Set<(msg: ChatMessage) => void>();
  private typingListeners = new Set<(id: string, isTyping: boolean) => void>();
  private presenceListeners = new Set<(id: string, status: PresenceStatus) => void>();

  async connect(sessionId: string): Promise<void> {
    this.sessionId = sessionId;
    this.presenceListeners.forEach((cb) => cb('physician', 'online'));
  }

  async sendMessage(text: string, clinical = false): Promise<ChatMessage> {
    if (!this.sessionId) throw new Error('Not connected');
    const message: ChatMessage = {
      id: crypto.randomUUID(),
      senderId: 'local',
      senderRole: 'patient',
      body: text,
      sentAt: new Date().toISOString(),
      clinical,
    };
    this.messages.push(message);
    this.messageListeners.forEach((cb) => cb(message));
    this.scheduleEcho(text);
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
    this.typingListeners.forEach((cb) => cb('local', isTyping));
  }

  async disconnect(): Promise<void> {
    if (this.typingTimer) window.clearTimeout(this.typingTimer);
    this.presenceListeners.forEach((cb) => cb('physician', 'offline'));
    this.sessionId = null;
    this.messages = [];
  }

  private scheduleEcho(originalText: string): void {
    this.typingListeners.forEach((cb) => cb('physician', true));
    this.typingTimer = window.setTimeout(() => {
      this.typingListeners.forEach((cb) => cb('physician', false));
      const echo: ChatMessage = {
        id: crypto.randomUUID(),
        senderId: 'physician-sim',
        senderRole: 'physician',
        body: `Echo: ${originalText}`,
        sentAt: new Date().toISOString(),
      };
      this.messages.push(echo);
      this.messageListeners.forEach((cb) => cb(echo));
    }, 800);
  }
}

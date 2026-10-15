export interface ChatMessage {
  id: string;
  senderId: string;
  senderRole: 'patient' | 'physician' | 'system';
  body: string;
  sentAt: string;
  clinical?: boolean;
}

export type PresenceStatus = 'online' | 'offline';

export interface ClinicalChatAdapter {
  connect(sessionId: string): Promise<void>;
  sendMessage(text: string, clinical?: boolean): Promise<ChatMessage>;
  getMessages(): ChatMessage[];
  onMessageReceived(callback: (msg: ChatMessage) => void): () => void;
  onTypingIndicator(callback: (participantId: string, isTyping: boolean) => void): () => void;
  onPresenceChange(callback: (participantId: string, status: PresenceStatus) => void): () => void;
  setTyping(isTyping: boolean): void;
  disconnect(): Promise<void>;
}

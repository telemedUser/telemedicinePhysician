import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import type { ChatMessage } from './types';
import { MessageBubble } from './MessageBubble';
import { TypingIndicator } from './TypingIndicator';

export interface ChatPanelProps {
  messages: ChatMessage[];
  typingVisible?: boolean;
  onSend: (text: string) => void;
  onTyping?: (isTyping: boolean) => void;
  disabled?: boolean;
}

export function ChatPanel({
  messages,
  typingVisible = false,
  onSend,
  onTyping,
  disabled = false,
}: ChatPanelProps) {
  const [draft, setDraft] = useState('');
  const typingTimeout = useRef<number | null>(null);

  const handleChange = (value: string) => {
    setDraft(value);
    onTyping?.(true);
    if (typingTimeout.current) window.clearTimeout(typingTimeout.current);
    typingTimeout.current = window.setTimeout(() => onTyping?.(false), 1500);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const text = draft.trim();
    if (!text || disabled) return;
    onSend(text);
    setDraft('');
    onTyping?.(false);
  };

  useEffect(
    () => () => {
      if (typingTimeout.current) window.clearTimeout(typingTimeout.current);
    },
    [],
  );

  return (
    <section aria-label="Clinical chat" data-testid="chat-panel" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div
        role="log"
        aria-live="polite"
        style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', padding: '0.5rem' }}
      >
        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} isOwn={msg.senderId === 'local'} />
        ))}
        <TypingIndicator visible={typingVisible} participantName="Physician" />
      </div>
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '0.5rem', padding: '0.5rem', borderTop: '1px solid #e5e7eb' }}>
        <input
          type="text"
          value={draft}
          onChange={(e) => handleChange(e.target.value)}
          placeholder="Type a message…"
          disabled={disabled}
          aria-label="Chat message"
          style={{ flex: 1, padding: '0.5rem' }}
        />
        <button type="submit" disabled={disabled || !draft.trim()}>
          Send
        </button>
      </form>
    </section>
  );
}

import type { ChatMessage } from './types';

export interface MessageBubbleProps {
  message: ChatMessage;
  isOwn?: boolean;
}

export function MessageBubble({ message, isOwn = false }: MessageBubbleProps) {
  return (
    <div
      data-testid={`message-${message.id}`}
      style={{
        alignSelf: isOwn ? 'flex-end' : 'flex-start',
        maxWidth: '75%',
        padding: '0.5rem 0.75rem',
        borderRadius: 12,
        background: isOwn ? '#dbeafe' : '#f3f4f6',
        marginBottom: '0.5rem',
      }}
    >
      <div style={{ fontSize: '0.75rem', color: '#6b7280', marginBottom: 4 }}>
        {message.senderRole}
        {message.clinical ? ' · clinical' : ''}
      </div>
      <div>{message.body}</div>
    </div>
  );
}

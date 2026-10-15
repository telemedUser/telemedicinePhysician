export interface TypingIndicatorProps {
  participantName?: string;
  visible?: boolean;
}

export function TypingIndicator({ participantName = 'Someone', visible = false }: TypingIndicatorProps) {
  if (!visible) return null;
  return (
    <div
      data-testid="typing-indicator"
      aria-live="polite"
      style={{ fontSize: '0.875rem', color: '#6b7280', fontStyle: 'italic', padding: '0.25rem 0' }}
    >
      {participantName} is typing…
    </div>
  );
}

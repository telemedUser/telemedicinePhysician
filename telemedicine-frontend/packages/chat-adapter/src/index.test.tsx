import { describe, expect, it, vi } from 'vitest';
import { SimulatorChatAdapter } from './SimulatorChatAdapter';
import { MessageBubble } from './MessageBubble';
import { TypingIndicator } from './TypingIndicator';
import { render, screen } from '@testing-library/react';

describe('chat-adapter', () => {
  it('echoes messages in simulator mode', async () => {
    vi.useFakeTimers();
    const adapter = new SimulatorChatAdapter();
    await adapter.connect('session-1');
    const received: string[] = [];
    adapter.onMessageReceived((msg) => received.push(msg.body));
    await adapter.sendMessage('Hello');
    vi.advanceTimersByTime(900);
    expect(received).toContain('Hello');
    expect(received.some((b) => b.startsWith('Echo:'))).toBe(true);
    vi.useRealTimers();
  });

  it('renders message bubble', () => {
    render(
      <MessageBubble
        message={{
          id: '1',
          senderId: 'local',
          senderRole: 'patient',
          body: 'Test',
          sentAt: new Date().toISOString(),
        }}
        isOwn
      />,
    );
    expect(screen.getByText('Test')).toBeInTheDocument();
  });

  it('shows typing indicator when visible', () => {
    render(<TypingIndicator visible participantName="Dr. Lee" />);
    expect(screen.getByTestId('typing-indicator')).toHaveTextContent('Dr. Lee is typing');
  });
});

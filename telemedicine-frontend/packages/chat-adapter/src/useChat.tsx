import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useChatAdapter } from './createChatAdapter';
import type { ChatMessage } from './types';

export interface UseChatOptions {
  sessionId: string;
  autoConnect?: boolean;
}

export function useChat({ sessionId, autoConnect = true }: UseChatOptions) {
  const adapter = useChatAdapter();
  const adapterRef = useRef(adapter);
  adapterRef.current = adapter;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [typing, setTyping] = useState(false);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const connect = useCallback(async () => {
    try {
      setError(null);
      await adapterRef.current.connect(sessionId);
      setMessages(adapterRef.current.getMessages());
      setConnected(true);
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
    }
  }, [sessionId]);

  const sendMessage = useCallback(async (text: string, clinical = false) => {
    const msg = await adapterRef.current.sendMessage(text, clinical);
    setMessages(adapterRef.current.getMessages());
    return msg;
  }, []);

  const setTypingStatus = useCallback((isTyping: boolean) => {
    adapterRef.current.setTyping(isTyping);
  }, []);

  const disconnect = useCallback(async () => {
    await adapterRef.current.disconnect();
    setConnected(false);
    setMessages([]);
  }, []);

  useEffect(() => {
    const unsubs = [
      adapter.onMessageReceived((msg) => setMessages((prev) => [...prev, msg])),
      adapter.onTypingIndicator((_id, isTyping) => setTyping(isTyping)),
    ];
    return () => unsubs.forEach((u) => u());
  }, [adapter]);

  useEffect(() => {
    if (autoConnect && sessionId) void connect();
    return () => {
      void adapterRef.current.disconnect();
    };
  }, [sessionId, autoConnect, connect]);

  return useMemo(
    () => ({
      messages,
      typing,
      connected,
      error,
      connect,
      sendMessage,
      setTyping: setTypingStatus,
      disconnect,
    }),
    [messages, typing, connected, error, connect, sendMessage, setTypingStatus, disconnect],
  );
}

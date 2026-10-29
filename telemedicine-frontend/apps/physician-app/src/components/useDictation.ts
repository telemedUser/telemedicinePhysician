import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Voice-to-text (dictation) via the browser Web Speech API. Returns whether dictation is supported,
 * whether it is currently listening, and start/stop/toggle controls. Recognised speech is delivered
 * through the onResult callback as finalised text chunks, so callers can append to a field value.
 *
 * No audio or transcript leaves the browser beyond what the platform's own speech service does for
 * SpeechRecognition; nothing is sent to our backend. Gracefully degrades: when the API is missing
 * (e.g. Firefox), isSupported is false and the mic control can be hidden.
 */

// The Web Speech API is not in the TS DOM lib under a stable name; describe the bits we use.
interface SpeechRecognitionResultLike {
  0: { transcript: string };
  isFinal: boolean;
}
interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: { length: number; [index: number]: SpeechRecognitionResultLike };
}
interface SpeechRecognitionLike {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((e: SpeechRecognitionEventLike) => void) | null;
  onerror: ((e: unknown) => void) | null;
  onend: (() => void) | null;
}
type SpeechRecognitionCtor = new () => SpeechRecognitionLike;

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  if (typeof window === 'undefined') return null;
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null;
}

export interface Dictation {
  isSupported: boolean;
  isListening: boolean;
  start: () => void;
  stop: () => void;
  toggle: () => void;
}

export function useDictation(onResult: (text: string) => void, lang = 'en-IN'): Dictation {
  const ctor = getRecognitionCtor();
  const isSupported = ctor != null;
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  // Keep the latest callback without re-creating the recognition instance.
  const onResultRef = useRef(onResult);
  onResultRef.current = onResult;

  const stop = useCallback(() => {
    const rec = recognitionRef.current;
    if (rec) {
      try {
        rec.stop();
      } catch {
        /* already stopped */
      }
    }
    setIsListening(false);
  }, []);

  const start = useCallback(() => {
    if (!ctor || recognitionRef.current) return;
    const rec = new ctor();
    rec.continuous = true;
    rec.interimResults = false;
    rec.lang = lang;
    rec.onresult = (e: SpeechRecognitionEventLike) => {
      let finalText = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i];
        if (r.isFinal) finalText += r[0].transcript;
      }
      if (finalText.trim()) onResultRef.current(finalText.trim());
    };
    rec.onerror = () => {
      recognitionRef.current = null;
      setIsListening(false);
    };
    rec.onend = () => {
      recognitionRef.current = null;
      setIsListening(false);
    };
    recognitionRef.current = rec;
    try {
      rec.start();
      setIsListening(true);
    } catch {
      recognitionRef.current = null;
      setIsListening(false);
    }
  }, [ctor, lang]);

  const toggle = useCallback(() => {
    if (isListening) stop();
    else start();
  }, [isListening, start, stop]);

  // Make sure recognition is torn down if the component unmounts mid-dictation.
  useEffect(() => {
    return () => {
      const rec = recognitionRef.current;
      if (rec) {
        try {
          rec.abort();
        } catch {
          /* ignore */
        }
        recognitionRef.current = null;
      }
    };
  }, []);

  return { isSupported, isListening, start, stop, toggle };
}

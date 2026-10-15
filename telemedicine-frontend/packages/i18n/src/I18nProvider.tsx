import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { EnMessages, Namespace } from './locales/en/index';
import {
  defaultMessages,
  detectLocale,
  getCachedLocale,
  loadLocale,
  persistLocale,
  type Locale,
} from './loadLocale';
import { formatCurrency, formatDate, formatNumber, formatTime, pluralize } from './formatters';

export type { Locale, Namespace };
export { formatDate, formatTime, formatNumber, formatCurrency, pluralize };

interface I18nContextValue {
  locale: Locale;
  messages: EnMessages;
  setLocale: (locale: Locale) => void;
  t: (namespace: Namespace, key: string, params?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function resolveKey(messages: EnMessages, namespace: Namespace, key: string): unknown {
  const parts = key.split('.');
  let node: unknown = messages[namespace];
  for (const part of parts) {
    if (node && typeof node === 'object' && part in (node as object)) {
      node = (node as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return node;
}

function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? `{${name}}`));
}

export interface I18nProviderProps {
  locale?: Locale;
  children: ReactNode;
}

export function I18nProvider({ locale: initialLocale, children }: I18nProviderProps) {
  const [locale, setLocaleState] = useState<Locale>(initialLocale ?? detectLocale());
  const [messages, setMessages] = useState<EnMessages>(
    getCachedLocale(locale) ?? defaultMessages,
  );

  useEffect(() => {
    let active = true;
    void loadLocale(locale).then((loaded) => {
      if (active) setMessages(loaded);
    });
    if (typeof document !== 'undefined') {
      document.documentElement.lang = locale;
      document.documentElement.dir = 'ltr';
    }
    return () => {
      active = false;
    };
  }, [locale]);

  const setLocale = useCallback((next: Locale) => {
    persistLocale(next);
    setLocaleState(next);
  }, []);

  const t = useCallback(
    (namespace: Namespace, key: string, params?: Record<string, string | number>) => {
      const value = resolveKey(messages, namespace, key);
      if (typeof value === 'string') return interpolate(value, params);
      return `${namespace}.${key}`;
    },
    [messages],
  );

  const value = useMemo(() => ({ locale, messages, setLocale, t }), [locale, messages, setLocale, t]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useTranslation(namespace: Namespace) {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useTranslation must be used within I18nProvider');
  return useMemo(
    () => ({
      t: (key: string, params?: Record<string, string | number>) => ctx.t(namespace, key, params),
      locale: ctx.locale,
      setLocale: ctx.setLocale,
      formatDate: (date: Date | string | number, style?: 'short' | 'medium' | 'long') =>
        formatDate(date, ctx.locale, style),
      formatTime: (date: Date | string | number) => formatTime(date, ctx.locale),
      formatNumber: (value: number, options?: Intl.NumberFormatOptions) =>
        formatNumber(value, ctx.locale, options),
      formatCurrency: (amount: number, currency?: string) =>
        formatCurrency(amount, ctx.locale, currency),
    }),
    [ctx, namespace],
  );
}

export interface TransProps {
  namespace: Namespace;
  i18nKey: string;
  params?: Record<string, string | number>;
  components?: Record<string, ReactNode>;
}

export function Trans({ namespace, i18nKey, params, components }: TransProps) {
  const { t } = useTranslation(namespace);
  const text = t(i18nKey, params);
  if (!components) return <>{text}</>;

  const parts = text.split(/(\{\w+\})/);
  return (
    <>
      {parts.map((part, i) => {
        const match = part.match(/^\{(\w+)\}$/);
        if (match && components[match[1]]) return <span key={i}>{components[match[1]]}</span>;
        return part ? <span key={i}>{part}</span> : null;
      })}
    </>
  );
}

/** Legacy global translate — prefer useTranslation(). */
let globalLocale: Locale = detectLocale();
let globalMessages: EnMessages = defaultMessages;

export function setLocale(locale: Locale): void {
  globalLocale = locale;
  persistLocale(locale);
  void loadLocale(locale).then((m) => {
    globalMessages = m;
  });
}

export function getLocale(): Locale {
  return globalLocale;
}

export function t(key: string): string {
  const [namespace, ...rest] = key.split('.') as [Namespace, ...string[]];
  const value = resolveKey(globalMessages, namespace, rest.join('.'));
  return typeof value === 'string' ? value : key;
}

export { loadLocale, detectLocale };

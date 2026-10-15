import type { EnMessages } from './locales/en/index';

export type Locale = 'en' | 'es-CL';

const loaders: Record<Locale, () => Promise<EnMessages>> = {
  en: () => import('./locales/en/index').then((m) => m.en),
  'es-CL': () => import('./locales/es-CL/index').then((m) => m.esCL),
};

const cache = new Map<Locale, EnMessages>();

export async function loadLocale(locale: Locale): Promise<EnMessages> {
  if (cache.has(locale)) return cache.get(locale)!;
  const messages = await loaders[locale]();
  cache.set(locale, messages);
  return messages;
}

export function detectLocale(): Locale {
  const envLocale =
    typeof import.meta !== 'undefined' && import.meta.env?.VITE_DEFAULT_LOCALE
      ? String(import.meta.env.VITE_DEFAULT_LOCALE)
      : null;
  if (envLocale === 'en' || envLocale === 'es-CL') return envLocale;

  try {
    const stored = localStorage.getItem('telemedicine.locale');
    if (stored === 'en' || stored === 'es-CL') return stored;
  } catch {
    /* ignore */
  }

  const nav = typeof navigator !== 'undefined' ? navigator.language : 'es-CL';
  if (nav.startsWith('en')) return 'en';
  return 'es-CL';
}

export function persistLocale(locale: Locale): void {
  try {
    localStorage.setItem('telemedicine.locale', locale);
  } catch {
    /* ignore */
  }
}

export function getCachedLocale(locale: Locale): EnMessages | null {
  return cache.get(locale) ?? null;
}

// Eager default for synchronous t() before provider mounts
export { en as defaultMessages } from './locales/en/index';

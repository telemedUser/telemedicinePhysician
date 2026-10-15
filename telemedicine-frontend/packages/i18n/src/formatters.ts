import type { Locale } from './loadLocale';

export function formatDate(date: Date | string | number, locale: Locale, style: 'short' | 'medium' | 'long' = 'medium'): string {
  const d = date instanceof Date ? date : new Date(date);
  const intlLocale = locale === 'es-CL' ? 'es-CL' : 'en';
  return new Intl.DateTimeFormat(intlLocale, { dateStyle: style }).format(d);
}

export function formatTime(date: Date | string | number, locale: Locale): string {
  const d = date instanceof Date ? date : new Date(date);
  const intlLocale = locale === 'es-CL' ? 'es-CL' : 'en';
  return new Intl.DateTimeFormat(intlLocale, { timeStyle: 'short' }).format(d);
}

export function formatNumber(value: number, locale: Locale, options?: Intl.NumberFormatOptions): string {
  const intlLocale = locale === 'es-CL' ? 'es-CL' : 'en';
  return new Intl.NumberFormat(intlLocale, options).format(value);
}

export function formatCurrency(amount: number, locale: Locale, currency = 'CLP'): string {
  const intlLocale = locale === 'es-CL' ? 'es-CL' : 'en';
  return new Intl.NumberFormat(intlLocale, { style: 'currency', currency }).format(amount);
}

export function pluralize(count: number, locale: Locale, forms: { one: string; other: string }): string {
  const intlLocale = locale === 'es-CL' ? 'es-CL' : 'en';
  const rule = new Intl.PluralRules(intlLocale).select(count);
  return rule === 'one' ? forms.one : forms.other;
}

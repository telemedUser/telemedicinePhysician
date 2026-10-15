export type { Locale } from './loadLocale';
export type { Namespace } from './locales/en/index';
export { en } from './locales/en/index';
export { esCL } from './locales/es-CL/index';
export {
  I18nProvider,
  useTranslation,
  Trans,
  setLocale,
  getLocale,
  t,
  formatDate,
  formatTime,
  formatNumber,
  formatCurrency,
  pluralize,
  loadLocale,
  detectLocale,
} from './I18nProvider';
export type { I18nProviderProps, TransProps } from './I18nProvider';

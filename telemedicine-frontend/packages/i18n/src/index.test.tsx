import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { I18nProvider, useTranslation } from './I18nProvider';
import { formatCurrency, formatDate } from './formatters';

function Sample() {
  const { t } = useTranslation('common');
  return <span>{t('loading')}</span>;
}

describe('i18n', () => {
  it('renders English strings', () => {
    render(
      <I18nProvider locale="en">
        <Sample />
      </I18nProvider>,
    );
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('renders Spanish strings', () => {
    render(
      <I18nProvider locale="es-CL">
        <Sample />
      </I18nProvider>,
    );
    expect(screen.getByText('Cargando...')).toBeInTheDocument();
  });

  it('formats dates and currency for es-CL', () => {
    const date = new Date('2026-08-24T12:00:00Z');
    expect(formatDate(date, 'es-CL', 'short')).toBeTruthy();
    expect(formatCurrency(1500, 'es-CL')).toContain('1');
  });
});

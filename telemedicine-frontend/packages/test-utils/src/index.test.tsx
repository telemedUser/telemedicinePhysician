import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { useTranslation } from '@telemedicine/i18n';
import { renderWithProviders } from './renderWithProviders';

function LocaleProbe() {
  const { t } = useTranslation('common');
  return <div data-testid="locale-probe">{t('loading')}</div>;
}

describe('test-utils', () => {
  it('renderWithProviders wraps all providers', () => {
    renderWithProviders(<LocaleProbe />);
    expect(screen.getByTestId('locale-probe')).toHaveTextContent('Loading...');
  });
});

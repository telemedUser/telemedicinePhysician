import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { I18nProvider } from '@telemedicine/i18n';
import { PlatformConsent } from './PlatformConsent';
import consentText from './consent-text';

describe('consent-kit', () => {
  it('loads versioned consent text from static JSON', () => {
    expect(consentText.platform.version).toBe('1.0.0');
    expect(consentText.platform.items).toHaveLength(2);
  });

  it('renders platform consent with i18n', () => {
    render(
      <I18nProvider locale="en">
        <PlatformConsent accepted={{}} onChange={vi.fn()} />
      </I18nProvider>,
    );
    expect(screen.getByRole('heading', { name: /platform terms/i })).toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from './theme/ThemeProvider';
import { Button } from './components/Button';
import { StatusBadge } from './components/StatusBadge';
import { Stepper } from './components/Navigation';

describe('design-system', () => {
  it('renders button', () => {
    render(
      <ThemeProvider theme="patient">
        <Button>Click me</Button>
      </ThemeProvider>,
    );
    expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument();
  });

  it('renders status badge', () => {
    render(<StatusBadge variant="ok">Active</StatusBadge>);
    expect(screen.getByRole('status')).toHaveTextContent('Active');
  });

  it('renders stepper with current step', () => {
    render(<Stepper steps={['One', 'Two', 'Three']} currentStep={1} />);
    expect(screen.getByRole('navigation', { name: 'Progress' })).toBeInTheDocument();
  });
});

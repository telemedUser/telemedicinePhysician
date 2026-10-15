import type { Meta, StoryObj } from '@storybook/react';
import { Button } from '../components/Button';
import { StatusBadge } from '../components/StatusBadge';
import { Stepper } from '../components/Navigation';
import { KpiCard, LoadingSpinner } from '../components/Feedback';
import { ThemeProvider } from '../theme/ThemeProvider';

const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  decorators: [(Story) => <ThemeProvider><Story /></ThemeProvider>],
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = { args: { children: 'Primary', variant: 'primary' } };
export const Secondary: Story = { args: { children: 'Secondary', variant: 'secondary' } };
export const Danger: Story = { args: { children: 'Delete', variant: 'danger' } };
export const Loading: Story = { args: { children: 'Save', loading: true } };

export const StatusBadges: StoryObj = {
  render: () => (
    <div style={{ display: 'flex', gap: 8 }}>
      <StatusBadge variant="ok">OK</StatusBadge>
      <StatusBadge variant="pending">Pending</StatusBadge>
      <StatusBadge variant="degraded">Degraded</StatusBadge>
    </div>
  ),
};

export const StepperExample: StoryObj = {
  render: () => <Stepper steps={['Consent', 'Identity', 'Waiting', 'Consultation']} currentStep={2} />,
};

export const KpiCardExample: StoryObj = {
  render: () => <KpiCard label="Active Sessions" value={42} trend="+5%" />,
};

export const Spinner: StoryObj = {
  render: () => <LoadingSpinner label="Loading data" />,
};

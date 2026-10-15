import type { Preview } from '@storybook/react';
import '../src/theme/tokens.css';

const preview: Preview = {
  parameters: {
    controls: { matchers: { color: /(background|color)$/i, date: /Date$/i } },
    a11y: { test: 'todo' },
  },
  globalTypes: {
    theme: {
      description: 'Global theme',
      defaultValue: 'patient',
      toolbar: {
        title: 'Theme',
        icon: 'paintbrush',
        items: [
          { value: 'patient', title: 'Patient' },
          { value: 'clinical', title: 'Clinical' },
        ],
      },
    },
  },
  decorators: [
    (Story, context) => (
      <div data-theme={context.globals.theme ?? 'patient'}>
        <Story />
      </div>
    ),
  ],
};

export default preview;

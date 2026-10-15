export { ThemeProvider, useTheme, type ThemeName, type ThemeProviderProps } from './theme/ThemeProvider';
export { Button, type ButtonProps, type ButtonVariant } from './components/Button';
export { StatusBadge, type StatusBadgeProps, type BadgeVariant } from './components/StatusBadge';
export { Modal, ConfirmDialog, type ModalProps, type ConfirmDialogProps } from './components/Modal';
export {
  ShellLayout,
  PageLayout,
  Stack,
  Inline,
  SplitPanel,
} from './components/Layout';
export { Stepper, Tabs, Breadcrumbs } from './components/Navigation';
export {
  LoadingSpinner,
  ErrorMessage,
  EmptyState,
  Skeleton,
  ToastContainer,
  KpiCard,
  MetricBar,
  VitalSign,
  TimerDisplay,
  ConsentBanner,
  type ToastMessage,
} from './components/Feedback';
export {
  FormField,
  TextField,
  TextArea,
  Select,
  Checkbox,
  SearchInput,
  DataTable,
  type FormFieldProps,
  type DataTableProps,
} from './components/Forms';

// Backward compatibility with shared-ui
export { ShellLayout as SharedShellLayout } from './components/Layout';
export { LoadingSpinner as SharedLoadingSpinner } from './components/Feedback';
export { ErrorMessage as SharedErrorMessage } from './components/Feedback';

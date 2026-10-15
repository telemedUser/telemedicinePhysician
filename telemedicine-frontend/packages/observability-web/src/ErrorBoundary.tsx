import { Component, type ErrorInfo, type ReactNode } from 'react';
import { ErrorMessage } from '@telemedicine/design-system';
import { reportError } from './reporting';

export interface ErrorBoundaryProps {
  fallback?: ReactNode | ((error: Error) => ReactNode);
  onError?: (error: Error, info: ErrorInfo) => void;
  correlationId?: string;
  journeyStep?: string;
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export class ObservabilityErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    this.props.onError?.(error, info);
    void reportError({
      message: error.message,
      stack: error.stack,
      componentStack: info.componentStack ?? undefined,
      correlationId: this.props.correlationId,
      journeyStep: this.props.journeyStep,
    });
  }

  render(): ReactNode {
    const { error } = this.state;
    if (error) {
      const { fallback } = this.props;
      if (typeof fallback === 'function') return fallback(error);
      if (fallback) return fallback;
      return <ErrorMessage title="Something went wrong" message={error.message} />;
    }
    return this.props.children;
  }
}

export { ObservabilityErrorBoundary as ErrorBoundary };

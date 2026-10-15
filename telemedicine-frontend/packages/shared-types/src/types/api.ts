export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: string;
  correlationId?: string;
  traceId?: string;
  timestamp?: string;
  violations?: Array<{ field: string; message: string }>;
}

export enum ErrorCode {
  SERVICE_UNAVAILABLE = 'TM-CORE-0001',
  RATE_LIMITED = 'TM-CORE-0002',
  CONSENT_REQUIRED = 'TM-CORE-0100',
  CONSENT_WITHDRAWN = 'TM-CORE-0101',
  CONSENT_VERSION_OUTDATED = 'TM-CORE-0102',
  IDENTITY_LOCKED_OUT = 'TM-CORE-0200',
  IDENTITY_2FA_FAILED = 'TM-CORE-0201',
  PAYMENT_NOT_CONFIRMED = 'TM-CORE-0300',
  VIDEO_PROVIDER_UNAVAILABLE = 'TM-CORE-0400',
  TELEMETRY_NOT_ENABLED = 'TM-CORE-0500',
  DOCUMENT_NOT_FOUND = 'TM-CORE-0600',
  SURVEY_ALREADY_SUBMITTED = 'TM-CORE-0700',
  CONFIG_KEY_NOT_FOUND = 'TM-CORE-0800',
  INTEGRATION_ERROR = 'TM-CORE-0900',
  INVALID_STATUS_TRANSITION = 'TM-CORE-1000',
  SESSION_LINK_EXPIRED = 'TM-CORE-1001',
  DEVICE_TEST_REQUIRED = 'TM-CORE-1002',
  NOT_IMPLEMENTED = 'TM-CORE-9999',
}

export const ERROR_HTTP_STATUS: Record<ErrorCode, number> = {
  [ErrorCode.SERVICE_UNAVAILABLE]: 503,
  [ErrorCode.RATE_LIMITED]: 429,
  [ErrorCode.CONSENT_REQUIRED]: 403,
  [ErrorCode.CONSENT_WITHDRAWN]: 403,
  [ErrorCode.CONSENT_VERSION_OUTDATED]: 409,
  [ErrorCode.IDENTITY_LOCKED_OUT]: 423,
  [ErrorCode.IDENTITY_2FA_FAILED]: 401,
  [ErrorCode.PAYMENT_NOT_CONFIRMED]: 402,
  [ErrorCode.VIDEO_PROVIDER_UNAVAILABLE]: 503,
  [ErrorCode.TELEMETRY_NOT_ENABLED]: 403,
  [ErrorCode.DOCUMENT_NOT_FOUND]: 404,
  [ErrorCode.SURVEY_ALREADY_SUBMITTED]: 409,
  [ErrorCode.CONFIG_KEY_NOT_FOUND]: 404,
  [ErrorCode.INTEGRATION_ERROR]: 502,
  [ErrorCode.INVALID_STATUS_TRANSITION]: 409,
  [ErrorCode.SESSION_LINK_EXPIRED]: 410,
  [ErrorCode.DEVICE_TEST_REQUIRED]: 412,
  [ErrorCode.NOT_IMPLEMENTED]: 501,
};

export function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    'type' in value &&
    'title' in value &&
    'status' in value
  );
}

export function synthesizeProblemDetail(
  status: number,
  code: ErrorCode = ErrorCode.SERVICE_UNAVAILABLE,
  detail?: string,
): ProblemDetail {
  return {
    type: `https://telemedicine.bupa/problems/${code}`,
    title: code,
    status,
    detail: detail ?? 'An unexpected error occurred',
    code,
    timestamp: new Date().toISOString(),
  };
}

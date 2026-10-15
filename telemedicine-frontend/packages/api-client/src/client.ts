import type { ApiResponse, ProblemDetail } from '@telemedicine/shared-types';
import { ErrorCode, isProblemDetail, synthesizeProblemDetail } from '@telemedicine/shared-types';

export interface RequestConfig extends RequestInit {
  correlationId?: string;
  idempotencyKey?: string;
  retry?: boolean;
  signal?: AbortSignal;
}

export type RequestInterceptor = (
  config: RequestConfig,
) => RequestConfig | Promise<RequestConfig>;

export type TokenRefresher = () => Promise<string | null>;

export class ApiError extends Error {
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title);
    this.name = 'ApiError';
    this.problem = problem;
  }
}

export class ApiClient {
  private readonly baseUrl: string;
  private readonly interceptors: { request: RequestInterceptor[] } = { request: [] };
  private tokenRefresher: TokenRefresher | null = null;
  private readonly maxRetries: number;
  private readonly controllers = new Set<AbortController>();

  constructor(baseUrl = '/api', maxRetries = 2) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
    this.maxRetries = maxRetries;
  }

  addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.interceptors.request.push(interceptor);
  }

  setTokenRefresher(refresher: TokenRefresher): void {
    this.tokenRefresher = refresher;
  }

  cancelAll(): void {
    for (const controller of this.controllers) {
      controller.abort();
    }
    this.controllers.clear();
  }

  async fetch(path: string, init: RequestConfig = {}): Promise<Response> {
    let config = await this.applyInterceptors(init);
    const controller = new AbortController();
    if (config.signal) {
      config.signal.addEventListener('abort', () => controller.abort());
    }
    this.controllers.add(controller);

    const url = `${this.baseUrl}${path}`;
    let attempt = 0;
    let lastError: unknown;

    while (attempt <= this.maxRetries) {
      try {
        const response = await fetch(url, { ...config, signal: controller.signal });
        if (response.status === 401 && this.tokenRefresher && attempt === 0) {
          const newToken = await this.tokenRefresher();
          if (newToken) {
            config = await this.applyInterceptors({
              ...config,
              headers: {
                ...(config.headers as Record<string, string>),
                Authorization: `Bearer ${newToken}`,
              },
            });
            attempt++;
            continue;
          }
        }
        if (response.ok || !this.shouldRetry(response.status, config)) {
          this.controllers.delete(controller);
          return response;
        }
        lastError = await this.parseError(response);
      } catch (err) {
        if ((err as Error).name === 'AbortError') throw err;
        lastError = err;
        if (!config.retry) break;
      }
      attempt++;
      if (attempt <= this.maxRetries) {
        await this.delay(this.backoffMs(attempt));
      }
    }

    this.controllers.delete(controller);
    throw lastError instanceof ApiError ? lastError : lastError;
  }

  async json<T>(path: string, init?: RequestConfig): Promise<T> {
    const response = await this.fetch(path, init);
    if (!response.ok) {
      throw await this.parseError(response);
    }
    return response.json() as Promise<T>;
  }

  async get<T>(path: string, init?: RequestConfig): Promise<ApiResponse<T>> {
    return this.json<ApiResponse<T>>(path, { ...init, method: 'GET' });
  }

  async post<T>(path: string, body?: unknown, init?: RequestConfig): Promise<ApiResponse<T>> {
    return this.json<ApiResponse<T>>(path, {
      ...init,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(init?.headers as Record<string, string>) },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  async put<T>(path: string, body?: unknown, init?: RequestConfig): Promise<ApiResponse<T>> {
    return this.json<ApiResponse<T>>(path, {
      ...init,
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...(init?.headers as Record<string, string>) },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  async delete<T>(path: string, init?: RequestConfig): Promise<ApiResponse<T>> {
    return this.json<ApiResponse<T>>(path, { ...init, method: 'DELETE' });
  }

  private async applyInterceptors(init: RequestConfig): Promise<RequestConfig> {
    let config: RequestConfig = {
      ...init,
      headers: { ...(init.headers as Record<string, string>) },
    };
    for (const interceptor of this.interceptors.request) {
      config = await interceptor(config);
    }
    return config;
  }

  private shouldRetry(status: number, config: RequestConfig): boolean {
    if (config.retry === false) return false;
    if (config.method && !['GET', 'HEAD', 'OPTIONS'].includes(config.method.toUpperCase())) {
      return config.idempotencyKey ? status >= 500 : false;
    }
    return status >= 500 || status === 429;
  }

  private async parseError(response: Response): Promise<ApiError> {
    const contentType = response.headers.get('content-type') ?? '';
    if (contentType.includes('application/problem+json') || contentType.includes('application/json')) {
      try {
        const body = await response.json();
        if (isProblemDetail(body)) {
          return new ApiError(body);
        }
      } catch {
        /* fall through */
      }
    }
    const retryAfter = response.headers.get('Retry-After');
    const code = response.status === 429 ? ErrorCode.RATE_LIMITED : ErrorCode.SERVICE_UNAVAILABLE;
    return new ApiError(
      synthesizeProblemDetail(
        response.status,
        code,
        retryAfter ? `Retry after ${retryAfter}s` : response.statusText,
      ),
    );
  }

  private backoffMs(attempt: number): number {
    const base = 200 * Math.pow(2, attempt - 1);
    const jitter = Math.random() * 100;
    return base + jitter;
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}

export function createAuthInterceptor(getToken: () => string | null): RequestInterceptor {
  return (config) => {
    const token = getToken();
    if (token) {
      config.headers = {
        ...(config.headers as Record<string, string>),
        Authorization: `Bearer ${token}`,
      };
    }
    return config;
  };
}

export function createCorrelationInterceptor(getId: () => string): RequestInterceptor {
  return (config) => {
    const id = config.correlationId ?? getId();
    config.headers = {
      ...(config.headers as Record<string, string>),
      'X-Correlation-Id': id,
    };
    return config;
  };
}

export function createIdempotencyInterceptor(getKey: () => string): RequestInterceptor {
  return (config) => {
    const key = config.idempotencyKey ?? getKey();
    config.headers = {
      ...(config.headers as Record<string, string>),
      'X-Idempotency-Key': key,
    };
    config.idempotencyKey = key;
    return config;
  };
}

export function createDefaultApiClient(getToken?: () => string | null): ApiClient {
  const baseUrl =
    typeof import.meta !== 'undefined' &&
    (import.meta as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL
      ? String((import.meta as { env: { VITE_API_BASE_URL: string } }).env.VITE_API_BASE_URL)
      : '/api';
  const client = new ApiClient(baseUrl);
  client.addRequestInterceptor(createCorrelationInterceptor(() => crypto.randomUUID()));
  if (getToken) {
    client.addRequestInterceptor(createAuthInterceptor(getToken));
  }
  return client;
}

export { ApiClient as default };

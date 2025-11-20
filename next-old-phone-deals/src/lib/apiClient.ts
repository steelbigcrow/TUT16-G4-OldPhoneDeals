const DEFAULT_API_BASE_URL = 'http://localhost:3000/api';

function getBaseUrl(): string {
  if (typeof process !== 'undefined' && process.env?.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }

  return DEFAULT_API_BASE_URL;
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type ApiRequestOptions = RequestInit & {
  skipJsonParse?: boolean;
  authToken?: string;
};

export class ApiError extends Error {
  status: number;

  body: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request<TResponse>(
  method: HttpMethod,
  path: string,
  body?: unknown,
  options: ApiRequestOptions = {}
): Promise<TResponse> {
  const baseUrl = getBaseUrl();
  const url = `${baseUrl}${path}`;
  const { skipJsonParse, authToken, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(fetchOptions.headers as Record<string, string> || {})
  };

  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  const response = await fetch(url, {
    ...fetchOptions,
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : fetchOptions.body
  });

  if (!response.ok) {
    let errorBody: unknown;
    let message = `Request failed with status ${response.status}`;

    try {
      errorBody = await response.json();

      if (
        errorBody &&
        typeof errorBody === 'object' &&
        'message' in (errorBody as Record<string, unknown>)
      ) {
        message = String((errorBody as Record<string, unknown>).message);
      }
    } catch {
      // ignore JSON parse error
    }

    throw new ApiError(message, response.status, errorBody);
  }

  if (skipJsonParse || response.status === 204) {
    return undefined as TResponse;
  }

  return (await response.json()) as TResponse;
}

export async function apiGet<TResponse>(
  path: string,
  options?: ApiRequestOptions
): Promise<TResponse> {
  return request<TResponse>('GET', path, undefined, options);
}

export async function apiPost<TResponse, TBody = unknown>(
  path: string,
  body: TBody,
  options?: ApiRequestOptions
): Promise<TResponse> {
  return request<TResponse>('POST', path, body, options);
}

export async function apiPut<TResponse, TBody = unknown>(
  path: string,
  body: TBody,
  options?: ApiRequestOptions
): Promise<TResponse> {
  return request<TResponse>('PUT', path, body, options);
}

export async function apiPatch<TResponse, TBody = unknown>(
  path: string,
  body: TBody,
  options?: ApiRequestOptions
): Promise<TResponse> {
  return request<TResponse>('PATCH', path, body, options);
}

export async function apiDelete<TResponse>(
  path: string,
  options?: ApiRequestOptions
): Promise<TResponse> {
  return request<TResponse>('DELETE', path, undefined, options);
}

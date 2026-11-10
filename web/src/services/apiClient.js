// fetch wrapper: attaches the Firebase ID token, converts RFC 9457 problem bodies into ApiError.
// AuthContext registers the token provider and the 401 handler, so this module has no Firebase import.

export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors = {} }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

let tokenProvider = async () => null;
let unauthorizedHandler = null;

export function setTokenProvider(fn) {
  tokenProvider = fn;
}

export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

const FALLBACK_MESSAGES = {
  0: 'Cannot reach the server. Check that the backend is running.',
  401: 'Your session has expired. Please sign in again.',
  403: 'You do not have permission to do this.',
  404: 'The requested item was not found.',
  500: 'Something went wrong on the server. Please try again.',
  503: 'The authentication service is temporarily unavailable. Please try again.',
};

// Empty in development (the Vite proxy forwards /api). Set VITE_API_BASE_URL at build time for a deployed API.
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '');

function buildUrl(path, params) {
  if (!params) return API_BASE + path;
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.append(key, String(value));
  });
  const qs = query.toString();
  return qs ? `${API_BASE}${path}?${qs}` : API_BASE + path;
}

function toApiError(status, body) {
  const fieldErrors = {};
  (body?.fieldErrors ?? []).forEach(({ field, message }) => {
    if (!fieldErrors[field]) fieldErrors[field] = message;
  });
  return new ApiError({
    status,
    code: body?.code ?? 'HTTP_' + status,
    message: body?.detail || FALLBACK_MESSAGES[status] || FALLBACK_MESSAGES[500],
    fieldErrors,
  });
}

export async function apiRequest(path, { method = 'GET', body, params } = {}) {
  const headers = { Accept: 'application/json' };
  const token = await tokenProvider();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(buildUrl(path, params), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', message: FALLBACK_MESSAGES[0] });
  }

  if (response.status === 204) return null;
  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const error = toApiError(response.status, data);
    if (response.status === 401 && unauthorizedHandler) unauthorizedHandler();
    throw error;
  }
  return data;
}

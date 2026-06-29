/**
 * Minimal fetch wrapper shared by all Bolts. On a non-2xx response it throws an Error carrying the
 * server's message (errors surface — never swallowed).
 */

export interface ApiError extends Error {
  status: number;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    const message =
      (data && (data.message || data.error)) || `Request failed with status ${response.status}`;
    const error = new Error(message) as ApiError;
    error.status = response.status;
    throw error;
  }

  return data as T;
}

/**
 * POSTs a raw text body (the file's contents) as {@code application/json}, used by import/preview
 * which send the picked file verbatim (U5, frontend-components §4.2). Parses the JSON response and
 * surfaces non-2xx as a thrown {@link ApiError}, mirroring {@link request}.
 */
async function postRaw<T>(path: string, rawBody: string): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: rawBody,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    const message =
      (data && (data.message || data.error)) || `Request failed with status ${response.status}`;
    const error = new Error(message) as ApiError;
    error.status = response.status;
    throw error;
  }

  return data as T;
}

/** Parses the {@code filename}/{@code filename*} (RFC 5987) of a Content-Disposition header. */
function parseFilename(disposition: string | null): string | null {
  if (!disposition) return null;
  // RFC 5987 form takes precedence (carries UTF-8, e.g. non-ASCII deck names).
  const star = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (star) {
    try {
      return decodeURIComponent(star[1]);
    } catch {
      // fall through to the plain filename
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  return plain ? plain[1] : null;
}

/**
 * Downloads {@code path} as a file: fetches a Blob, reads the server's Content-Disposition filename
 * (falling back to {@code fallbackName}), and triggers a browser download via a temporary anchor,
 * revoking the object URL afterward (U5 export, frontend-components §4.1).
 */
async function download(path: string, fallbackName = 'deck.json'): Promise<void> {
  const response = await fetch(path);
  if (!response.ok) {
    const text = await response.text();
    let message = `Request failed with status ${response.status}`;
    try {
      const data = text ? JSON.parse(text) : undefined;
      if (data && (data.message || data.error)) message = data.message || data.error;
    } catch {
      // non-JSON error body — keep the default message
    }
    const error = new Error(message) as ApiError;
    error.status = response.status;
    throw error;
  }

  const blob = await response.blob();
  const filename = parseFilename(response.headers.get('Content-Disposition')) ?? fallbackName;
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export const apiClient = {
  get: <T>(path: string): Promise<T> => request<T>('GET', path),
  post: <T>(path: string, body?: unknown): Promise<T> => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown): Promise<T> => request<T>('PUT', path, body),
  delete: <T = void>(path: string): Promise<T> => request<T>('DELETE', path),
  /** POST a raw text body as application/json (import/preview send the file text verbatim). */
  postRaw: <T>(path: string, rawBody: string): Promise<T> => postRaw<T>(path, rawBody),
  /** Download a path as a file, honoring the server's Content-Disposition filename. */
  download: (path: string, fallbackName?: string): Promise<void> => download(path, fallbackName),
};

import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient, ApiError } from './apiClient';

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    text: () => Promise.resolve(body === undefined ? '' : JSON.stringify(body)),
  } as Response);
}

describe('apiClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns parsed JSON on success', async () => {
    vi.stubGlobal('fetch', mockFetch(200, { id: '1', name: 'English' }));
    const result = await apiClient.get<{ id: string; name: string }>('/api/decks/1');
    expect(result).toEqual({ id: '1', name: 'English' });
  });

  it('sends a JSON body on post', async () => {
    const fetchMock = mockFetch(201, { id: '1' });
    vi.stubGlobal('fetch', fetchMock);
    await apiClient.post('/api/decks', { name: 'New' });
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/decks',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'New' }),
      }),
    );
  });

  it('throws an error carrying the server message and status on failure', async () => {
    vi.stubGlobal('fetch', mockFetch(404, { status: 404, message: 'Deck not found: x' }));
    await expect(apiClient.get('/api/decks/x')).rejects.toMatchObject({
      message: 'Deck not found: x',
    });
  });

  it('exposes the HTTP status on the thrown error', async () => {
    vi.stubGlobal('fetch', mockFetch(400, { status: 400, message: 'name: must not be blank' }));
    try {
      await apiClient.post('/api/decks', { name: '' });
      throw new Error('should have thrown');
    } catch (e) {
      expect((e as ApiError).status).toBe(400);
    }
  });

  it('sends a JSON body on put', async () => {
    const fetchMock = mockFetch(200, { id: '1', name: 'Renamed' });
    vi.stubGlobal('fetch', fetchMock);
    await apiClient.put('/api/decks/1', { name: 'Renamed' });
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/decks/1',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ name: 'Renamed' }),
      }),
    );
  });

  it('issues a DELETE with no body and resolves on 204', async () => {
    const fetchMock = mockFetch(204, undefined);
    vi.stubGlobal('fetch', fetchMock);
    await expect(apiClient.delete('/api/decks/1')).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/decks/1',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });

  it('postRaw sends the raw text body as application/json', async () => {
    const fetchMock = mockFetch(200, { valid: true });
    vi.stubGlobal('fetch', fetchMock);
    await apiClient.postRaw('/api/decks/import/preview', '{"version":"1.0"}');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/decks/import/preview',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: '{"version":"1.0"}',
      }),
    );
  });

  it('postRaw throws the server message and status on failure', async () => {
    vi.stubGlobal(
      'fetch',
      mockFetch(400, { status: 400, message: 'Invalid WordBook export format' }),
    );
    await expect(apiClient.postRaw('/api/decks/import', 'garbage')).rejects.toMatchObject({
      message: 'Invalid WordBook export format',
      status: 400,
    });
  });

  it('download fetches a blob, reads the Content-Disposition filename, and triggers a download', async () => {
    const blob = new Blob(['{}'], { type: 'application/json' });
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(blob),
      headers: {
        get: (key: string) =>
          key === 'Content-Disposition'
            ? 'attachment; filename="Spanish_2025-01-01.json"; filename*=UTF-8\'\'Spanish_2025-01-01.json'
            : null,
      },
    });
    vi.stubGlobal('fetch', fetchMock);
    const createObjectURL = vi.fn().mockReturnValue('blob:mock');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL } as unknown as typeof URL);

    const anchor = document.createElement('a');
    const clickSpy = vi.fn();
    anchor.click = clickSpy;
    const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(anchor);

    await apiClient.download('/api/decks/1/export', 'fallback.json');

    expect(fetchMock).toHaveBeenCalledWith('/api/decks/1/export');
    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(anchor.download).toBe('Spanish_2025-01-01.json');
    expect(clickSpy).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock');

    createElementSpy.mockRestore();
  });

  it('download falls back to the provided name when no Content-Disposition is present', async () => {
    const blob = new Blob(['{}'], { type: 'application/json' });
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        blob: () => Promise.resolve(blob),
        headers: { get: () => null },
      }),
    );
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn().mockReturnValue('blob:mock'),
      revokeObjectURL: vi.fn(),
    } as unknown as typeof URL);

    const anchor = document.createElement('a');
    anchor.click = vi.fn();
    const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(anchor);

    await apiClient.download('/api/decks/1/export', 'fallback.json');
    expect(anchor.download).toBe('fallback.json');

    createElementSpy.mockRestore();
  });

  it('download throws the server message on a non-2xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        text: () => Promise.resolve(JSON.stringify({ status: 404, message: 'Deck not found: x' })),
      }),
    );
    await expect(apiClient.download('/api/decks/x/export')).rejects.toMatchObject({
      message: 'Deck not found: x',
      status: 404,
    });
  });
});

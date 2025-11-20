import { apiGet, apiPost, ApiError } from '@/lib/apiClient';

describe('apiClient', () => {
  const globalAny = global as unknown as { fetch: jest.Mock };

  beforeEach(() => {
    globalAny.fetch = jest.fn();
  });

  it('apiGet returns JSON data when response is ok', async () => {
    const mockBody = { foo: 'bar' };

    globalAny.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockBody,
    });

    const result = await apiGet<typeof mockBody>('/test');

    expect(result).toEqual(mockBody);
    expect(globalAny.fetch).toHaveBeenCalledWith(
      'http://localhost:3000/api/test',
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('adds Authorization header when authToken is provided', async () => {
    globalAny.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({}),
    });

    await apiGet('/with-auth', {
      authToken: 'token-123',
      headers: { 'X-Custom': '1' },
    });

    const [, options] = globalAny.fetch.mock.calls[0];
    expect(options.headers).toMatchObject({
      'Content-Type': 'application/json',
      Authorization: 'Bearer token-123',
      'X-Custom': '1',
    });
  });

  it('throws ApiError with status and message from body', async () => {
    const errorBody = { message: 'Invalid request' };

    globalAny.fetch.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => errorBody,
    });

    await expect(apiGet('/error')).rejects.toBeInstanceOf(ApiError);
    await expect(apiGet('/error')).rejects.toEqual(
      expect.objectContaining({
        status: 400,
        body: errorBody,
        message: 'Invalid request',
      }),
    );
  });

  it('uses default message when error body has no message', async () => {
    globalAny.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({}),
    });

    await expect(apiGet('/error-500')).rejects.toEqual(
      expect.objectContaining({
        status: 500,
        message: 'Request failed with status 500',
      }),
    );
  });

  it('supports POST requests via apiPost', async () => {
    const body = { hello: 'world' };
    globalAny.fetch.mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => body,
    });

    const result = await apiPost<typeof body, typeof body>('/post', body);

    expect(result).toEqual(body);
    expect(globalAny.fetch).toHaveBeenCalledWith(
      'http://localhost:3000/api/post',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});

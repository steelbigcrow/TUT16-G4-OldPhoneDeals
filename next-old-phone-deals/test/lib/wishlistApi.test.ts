import { addToWishlist, getWishlist, removeFromWishlist } from '@/lib/wishlistApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiDelete: jest.fn()
}));

describe('wishlistApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
    apiDelete: jest.Mock;
  };

  const token = 'tk';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('normalizes wishlist response when totalItems is missing', async () => {
    const responseData = {
      userId: 'u1',
      phones: [
        {
          id: 'p1',
          title: 'Phone 1',
          brand: 'Apple',
          price: 499,
          stock: 2
        }
      ]
    };
    api.apiGet.mockResolvedValue({ success: true, data: responseData });

    const result = await getWishlist(token);

    expect(api.apiGet).toHaveBeenCalledWith('/wishlist', { authToken: token });
    expect(result).toEqual({
      ...responseData,
      totalItems: 1
    });
  });

  it('throws when getWishlist fails', async () => {
    api.apiGet.mockResolvedValue({
      success: false,
      message: 'Failed to get wishlist'
    });

    await expect(getWishlist(token)).rejects.toThrow('Failed to get wishlist');
  });

  it('adds item to wishlist and respects provided totals', async () => {
    const responseData = {
      userId: 'u1',
      phones: undefined,
      totalItems: 3
    };
    api.apiPost.mockResolvedValue({ success: true, data: responseData });

    const result = await addToWishlist(token, 'p2');

    expect(api.apiPost).toHaveBeenCalledWith(
      '/wishlist',
      { phoneId: 'p2' },
      { authToken: token }
    );
    expect(result).toEqual({
      userId: 'u1',
      phones: [],
      totalItems: 3
    });
  });

  it('removes item from wishlist and normalizes empty lists', async () => {
    const responseData = {
      userId: 'u1',
      phones: [],
      totalItems: 0
    };
    api.apiDelete.mockResolvedValue({ success: true, data: responseData });

    const result = await removeFromWishlist(token, 'p2');

    expect(api.apiDelete).toHaveBeenCalledWith('/wishlist/p2', {
      authToken: token
    });
    expect(result).toEqual(responseData);
  });

  it('throws when add/remove operations fail', async () => {
    api.apiPost.mockResolvedValue({ success: false, message: 'bad add' });
    await expect(addToWishlist(token, 'p3')).rejects.toThrow('bad add');

    api.apiDelete.mockResolvedValue({ success: false, message: 'bad remove' });
    await expect(removeFromWishlist(token, 'p4')).rejects.toThrow('bad remove');
  });
});

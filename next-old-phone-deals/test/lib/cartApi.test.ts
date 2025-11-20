import { addToCart, getCart, removeCartItem, updateCartItem } from '@/lib/cartApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiDelete: jest.fn()
}));

describe('cartApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
    apiPut: jest.Mock;
    apiDelete: jest.Mock;
  };
  const token = 'tk';
  const cart = {
    id: 'c1',
    userId: 'u1',
    items: [
      { phoneId: 'p1', title: 'Phone', quantity: 1, price: 199 }
    ],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gets cart items', async () => {
    api.apiGet.mockResolvedValue({ success: true, data: cart });

    const result = await getCart(token);

    expect(api.apiGet).toHaveBeenCalledWith('/cart', { authToken: token });
    expect(result).toEqual(cart);
  });

  it('adds item to cart', async () => {
    api.apiPost.mockResolvedValue({ success: true, data: cart });

    const payload = { phoneId: 'p2', quantity: 2 };
    const result = await addToCart(token, payload as any);

    expect(api.apiPost).toHaveBeenCalledWith('/cart', payload, { authToken: token });
    expect(result).toEqual(cart);
  });

  it('updates cart item quantity', async () => {
    api.apiPut.mockResolvedValue({ success: true, data: cart });

    const result = await updateCartItem(token, 'p1', 3);

    expect(api.apiPut).toHaveBeenCalledWith(
      '/cart/p1',
      { quantity: 3 },
      { authToken: token }
    );
    expect(result).toEqual(cart);
  });

  it('removes a cart item', async () => {
    api.apiDelete.mockResolvedValue({ success: true, data: cart });

    const result = await removeCartItem(token, 'p1');

    expect(api.apiDelete).toHaveBeenCalledWith('/cart/p1', { authToken: token });
    expect(result).toEqual(cart);
  });

  it('throws when cart operations fail', async () => {
    api.apiGet.mockResolvedValue({ success: false, message: 'load failed' });
    await expect(getCart(token)).rejects.toThrow('load failed');

    api.apiPost.mockResolvedValue({ success: false, message: 'add failed' });
    await expect(addToCart(token, { phoneId: 'p1', quantity: 1 } as any)).rejects.toThrow(
      'add failed'
    );

    api.apiPut.mockResolvedValue({ success: false, message: 'update failed' });
    await expect(updateCartItem(token, 'p1', 1)).rejects.toThrow('update failed');

    api.apiDelete.mockResolvedValue({ success: false, message: 'remove failed' });
    await expect(removeCartItem(token, 'p1')).rejects.toThrow('remove failed');
  });
});

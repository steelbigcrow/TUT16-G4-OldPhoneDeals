import { createOrder, getOrderById, getUserOrders } from '@/lib/orderApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn()
}));

describe('orderApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
  };
  const token = 'tk';
  const order = {
    id: 'o1',
    userId: 'u1',
    items: [{ phoneId: 'p1', title: 'Phone', quantity: 1, price: 199 }],
    totalAmount: 199,
    address: {
      street: '123 Main',
      city: 'Sydney',
      state: 'NSW',
      zip: '2000',
      country: 'AU'
    },
    createdAt: new Date().toISOString()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('creates an order via checkout', async () => {
    api.apiPost.mockResolvedValue({ success: true, data: order });

    const payload = { address: order.address };
    const result = await createOrder(token, payload as any);

    expect(api.apiPost).toHaveBeenCalledWith('/orders/checkout', payload, {
      authToken: token
    });
    expect(result).toEqual(order);
  });

  it('fetches user orders', async () => {
    api.apiGet.mockResolvedValue({ success: true, data: [order] });

    const result = await getUserOrders(token, 'u1');

    expect(api.apiGet).toHaveBeenCalledWith('/orders/user/u1', {
      authToken: token
    });
    expect(result).toEqual([order]);
  });

  it('fetches order by id', async () => {
    api.apiGet.mockResolvedValue({ success: true, data: order });

    const result = await getOrderById(token, 'o1');

    expect(api.apiGet).toHaveBeenCalledWith('/orders/o1', { authToken: token });
    expect(result).toEqual(order);
  });

  it('throws when any order call fails', async () => {
    api.apiPost.mockResolvedValue({ success: false, message: 'checkout failed' });
    await expect(createOrder(token, { address: order.address } as any)).rejects.toThrow(
      'checkout failed'
    );

    api.apiGet.mockResolvedValue({ success: false, message: 'user orders failed' });
    await expect(getUserOrders(token, 'u1')).rejects.toThrow('user orders failed');

    api.apiGet.mockResolvedValue({ success: false, message: 'order failed' });
    await expect(getOrderById(token, 'o1')).rejects.toThrow('order failed');
  });
});

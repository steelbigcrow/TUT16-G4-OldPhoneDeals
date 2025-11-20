import { getAdminOrders, getOrderDetail } from '@/lib/adminOrderApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminOrderApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
    apiPut: jest.Mock;
    apiPatch: jest.Mock;
    apiDelete: jest.Mock;
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getAdminOrders', () => {
    it('calls apiGet with default pagination and returns data', async () => {
      const token = 't-1';
      const pageData = {
        content: [{ id: 'o1' }],
        currentPage: 1,
        totalPages: 1,
        totalItems: 1,
        itemsPerPage: 10,
        hasNext: false,
        hasPrevious: false
      };
      api.apiGet.mockResolvedValue({ success: true, data: pageData });

      const result = await getAdminOrders(token);

      expect(result).toEqual(pageData);
      expect(api.apiGet).toHaveBeenCalledTimes(1);
      expect(api.apiGet).toHaveBeenCalledWith('/admin/orders?page=0&pageSize=10', { authToken: token });
    });

    it('throws when backend returns success=false', async () => {
      const token = 't-1';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch orders' });

      await expect(getAdminOrders(token)).rejects.toThrow('Failed to fetch orders');
      expect(api.apiGet).toHaveBeenCalledWith('/admin/orders?page=0&pageSize=10', { authToken: token });
    });
  });

  describe('getOrderDetail', () => {
    it('calls apiGet with orderId and returns data', async () => {
      const token = 't-2';
      const orderId = 'order-123';
      const data = { id: orderId, status: 'PAID' };
      api.apiGet.mockResolvedValue({ success: true, data });

      const result = await getOrderDetail(token, orderId);

      expect(result).toEqual(data);
      expect(api.apiGet).toHaveBeenCalledTimes(1);
      expect(api.apiGet).toHaveBeenCalledWith(`/admin/orders/${orderId}`, { authToken: token });
    });

    it('throws when backend indicates failure', async () => {
      const token = 't-2';
      const orderId = 'order-123';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch order detail' });

      await expect(getOrderDetail(token, orderId)).rejects.toThrow('Failed to fetch order detail');
      expect(api.apiGet).toHaveBeenCalledWith(`/admin/orders/${orderId}`, { authToken: token });
    });
  });
});

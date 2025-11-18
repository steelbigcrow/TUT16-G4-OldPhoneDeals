import { getAdminPhones, updateAdminPhone, toggleAdminPhoneDisabled, deleteAdminPhone } from './adminPhoneApi';

jest.mock('./apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminPhoneApi', () => {
  const api = jest.requireMock('./apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
    apiPut: jest.Mock;
    apiPatch: jest.Mock;
    apiDelete: jest.Mock;
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getAdminPhones', () => {
    it('使用默认分页并返回数据', async () => {
      const token = 'tk';
      const pageData = { content: [{ id: 'p1' }], currentPage: 0, totalPages: 1, total: 1, pageSize: 10 };
      api.apiGet.mockResolvedValue({ success: true, data: pageData });

      const result = await getAdminPhones(token);

      expect(result).toEqual(pageData);
      expect(api.apiGet).toHaveBeenCalledTimes(1);
      expect(api.apiGet).toHaveBeenCalledWith('/admin/phones?page=0&pageSize=10', { authToken: token });
    });

    it('后端返回 success=false 时抛错', async () => {
      const token = 'tk';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch phones' });

      await expect(getAdminPhones(token)).rejects.toThrow('Failed to fetch phones');
      expect(api.apiGet).toHaveBeenCalledWith('/admin/phones?page=0&pageSize=10', { authToken: token });
    });
  });

  describe('updateAdminPhone', () => {
    it('PUT 更新并返回数据', async () => {
      const token = 'tk';
      const phoneId = 'pid-1';
      const body = { title: 'New', price: 100 };
      const data = { id: phoneId, title: 'New', price: 100, stock: 1, brand: 'APPLE', salesCount: 0, isDisabled: false, sellerId: 's1', sellerEmail: 'e', sellerName: 'n', createdAt: new Date().toISOString(), reviewCount: 0, averageRating: 0 };
      api.apiPut.mockResolvedValue({ success: true, data });

      const result = await updateAdminPhone(token, phoneId, body as any);

      expect(result).toEqual(data);
      expect(api.apiPut).toHaveBeenCalledTimes(1);
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/phones/${phoneId}`, body, { authToken: token });
    });

    it('失败时抛出后端消息', async () => {
      const token = 'tk';
      const phoneId = 'pid-1';
      api.apiPut.mockResolvedValue({ success: false, message: 'Failed to update phone' });

      await expect(updateAdminPhone(token, phoneId, {} as any)).rejects.toThrow('Failed to update phone');
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/phones/${phoneId}`, {}, { authToken: token });
    });
  });

  describe('toggleAdminPhoneDisabled', () => {
    it('PUT 切换禁用状态并返回数据', async () => {
      const token = 'tk';
      const phoneId = 'pid-2';
      const data = { id: phoneId, isDisabled: true };
      api.apiPut.mockResolvedValue({ success: true, data });

      const result = await toggleAdminPhoneDisabled(token, phoneId);

      expect(result).toEqual(data);
      expect(api.apiPut).toHaveBeenCalledTimes(1);
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/phones/${phoneId}/toggle-disabled`, {}, { authToken: token });
    });

    it('失败时抛错', async () => {
      const token = 'tk';
      const phoneId = 'pid-2';
      api.apiPut.mockResolvedValue({ success: false, message: 'Failed to toggle phone status' });

      await expect(toggleAdminPhoneDisabled(token, phoneId)).rejects.toThrow('Failed to toggle phone status');
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/phones/${phoneId}/toggle-disabled`, {}, { authToken: token });
    });
  });

  describe('deleteAdminPhone', () => {
    it('DELETE 成功不抛错', async () => {
      const token = 'tk';
      const phoneId = 'pid-3';
      api.apiDelete.mockResolvedValue({ success: true });

      await expect(deleteAdminPhone(token, phoneId)).resolves.toBeUndefined();
      expect(api.apiDelete).toHaveBeenCalledTimes(1);
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/phones/${phoneId}`, { authToken: token });
    });

    it('success=false 时抛错', async () => {
      const token = 'tk';
      const phoneId = 'pid-3';
      api.apiDelete.mockResolvedValue({ success: false, message: 'Failed to delete phone' });

      await expect(deleteAdminPhone(token, phoneId)).rejects.toThrow('Failed to delete phone');
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/phones/${phoneId}`, { authToken: token });
    });
  });
});
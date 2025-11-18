import { getAdminUsers, getUserDetail, updateUser, toggleUserDisabled, deleteUser } from './adminUserApi';

jest.mock('./apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminUserApi', () => {
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

  describe('getAdminUsers', () => {
    it('使用默认分页并返回数据', async () => {
      const token = 'tk';
      const pageData = { content: [{ id: 'u1' }], currentPage: 0, totalPages: 1, total: 1, pageSize: 10 };
      api.apiGet.mockResolvedValue({ success: true, data: pageData });

      const result = await getAdminUsers(token);

      expect(result).toEqual(pageData);
      expect(api.apiGet).toHaveBeenCalledTimes(1);
      expect(api.apiGet).toHaveBeenCalledWith('/admin/users?page=0&pageSize=10', { authToken: token });
    });

    it('后端返回 success=false 时抛错', async () => {
      const token = 'tk';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch users' });

      await expect(getAdminUsers(token)).rejects.toThrow('Failed to fetch users');
      expect(api.apiGet).toHaveBeenCalledWith('/admin/users?page=0&pageSize=10', { authToken: token });
    });
  });

  describe('getUserDetail', () => {
    it('GET 获取用户详情', async () => {
      const token = 'tk';
      const userId = 'u-1';
      const data = { id: userId, email: 'a@b.com', firstName: 'A', lastName: 'B', isEmailVerified: true, isDisabled: false, createdAt: new Date().toISOString(), listingsCount: 1, reviewsCount: 2, ordersCount: 3 };
      api.apiGet.mockResolvedValue({ success: true, data });

      const result = await getUserDetail(token, userId);

      expect(result).toEqual(data);
      expect(api.apiGet).toHaveBeenCalledWith(`/admin/users/${userId}`, { authToken: token });
    });

    it('失败时抛错', async () => {
      const token = 'tk';
      const userId = 'u-1';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch user detail' });

      await expect(getUserDetail(token, userId)).rejects.toThrow('Failed to fetch user detail');
      expect(api.apiGet).toHaveBeenCalledWith(`/admin/users/${userId}`, { authToken: token });
    });
  });

  describe('updateUser', () => {
    it('PUT 更新用户并返回数据', async () => {
      const token = 'tk';
      const userId = 'u-2';
      const body = { firstName: 'New' };
      const data = { id: userId, email: 'x@y.com', firstName: 'New', lastName: 'Z', isEmailVerified: true, isDisabled: false, createdAt: new Date().toISOString() };
      api.apiPut.mockResolvedValue({ success: true, data });

      const result = await updateUser(token, userId, body as any);

      expect(result).toEqual(data);
      expect(api.apiPut).toHaveBeenCalledTimes(1);
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/users/${userId}`, body, { authToken: token });
    });

    it('失败时抛错', async () => {
      const token = 'tk';
      const userId = 'u-2';
      api.apiPut.mockResolvedValue({ success: false, message: 'Failed to update user' });

      await expect(updateUser(token, userId, {} as any)).rejects.toThrow('Failed to update user');
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/users/${userId}`, {}, { authToken: token });
    });
  });

  describe('toggleUserDisabled', () => {
    it('PUT 切换禁用状态并返回数据', async () => {
      const token = 'tk';
      const userId = 'u-3';
      const data = { id: userId, isDisabled: true };
      api.apiPut.mockResolvedValue({ success: true, data });

      const result = await toggleUserDisabled(token, userId);

      expect(result).toEqual(data);
      expect(api.apiPut).toHaveBeenCalledTimes(1);
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/users/${userId}/toggle-disabled`, {}, { authToken: token });
    });

    it('失败时抛错', async () => {
      const token = 'tk';
      const userId = 'u-3';
      api.apiPut.mockResolvedValue({ success: false, message: 'Failed to toggle user status' });

      await expect(toggleUserDisabled(token, userId)).rejects.toThrow('Failed to toggle user status');
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/users/${userId}/toggle-disabled`, {}, { authToken: token });
    });
  });

  describe('deleteUser', () => {
    it('DELETE 成功时不抛错', async () => {
      const token = 'tk';
      const userId = 'u-4';
      api.apiDelete.mockResolvedValue({ success: true });

      await expect(deleteUser(token, userId)).resolves.toBeUndefined();
      expect(api.apiDelete).toHaveBeenCalledTimes(1);
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/users/${userId}`, { authToken: token });
    });

    it('success=false 时抛错', async () => {
      const token = 'tk';
      const userId = 'u-4';
      api.apiDelete.mockResolvedValue({ success: false, message: 'Failed to delete user' });

      await expect(deleteUser(token, userId)).rejects.toThrow('Failed to delete user');
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/users/${userId}`, { authToken: token });
    });
  });
});
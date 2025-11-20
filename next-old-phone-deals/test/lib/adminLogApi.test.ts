import { getAdminLogs } from '@/lib/adminLogApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminLogApi', () => {
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

  it('getAdminLogs 使用默认分页参数并返回数据', async () => {
    const token = 't-1';
    const pageData = {
      content: [{ id: 'log1' }],
      currentPage: 1,
      totalPages: 1,
      totalItems: 1,
      itemsPerPage: 10,
      hasNext: false,
      hasPrevious: false
    };
    api.apiGet.mockResolvedValue({ success: true, data: pageData });

    const result = await getAdminLogs(token);

    expect(result).toEqual(pageData);
    expect(api.apiGet).toHaveBeenCalledTimes(1);
    expect(api.apiGet).toHaveBeenCalledWith('/admin/logs?page=0&pageSize=10', { authToken: token });
  });

  it('getAdminLogs 失败时抛出后端消息', async () => {
    const token = 't-1';
    api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch admin logs' });

    await expect(getAdminLogs(token)).rejects.toThrow('Failed to fetch admin logs');
    expect(api.apiGet).toHaveBeenCalledWith('/admin/logs?page=0&pageSize=10', { authToken: token });
  });
});

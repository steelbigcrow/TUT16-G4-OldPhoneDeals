import { getDashboardStats } from './adminDashboardApi';

jest.mock('./apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminDashboardApi', () => {
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

  it('getDashboardStats calls apiGet with auth and returns data', async () => {
    const token = 'token-abc';
    const data = { ordersToday: 12, newUsers: 3 };
    api.apiGet.mockResolvedValue({ success: true, data });

    const result = await getDashboardStats(token);

    expect(result).toEqual(data);
    expect(api.apiGet).toHaveBeenCalledTimes(1);
    expect(api.apiGet).toHaveBeenCalledWith('/admin/dashboard-stats', { authToken: token });
  });

  it('getDashboardStats throws when success=false', async () => {
    const token = 'token-abc';
    api.apiGet.mockResolvedValue({ success: false, message: 'Failed' });

    await expect(getDashboardStats(token)).rejects.toThrow('Failed');
    expect(api.apiGet).toHaveBeenCalledWith('/admin/dashboard-stats', { authToken: token });
  });
});
import { getAdminReviews, toggleReviewVisibility, deleteReview } from '@/lib/adminReviewApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiPatch: jest.fn(),
  apiDelete: jest.fn(),
}));

describe('adminReviewApi', () => {
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

  describe('getAdminReviews', () => {
    it('使用默认分页并返回数据', async () => {
      const token = 'tk';
      const pageData = {
        content: [{ id: 'r1' }],
        currentPage: 1,
        totalPages: 1,
        totalItems: 1,
        itemsPerPage: 10,
        hasNext: false,
        hasPrevious: false
      };
      api.apiGet.mockResolvedValue({ success: true, data: pageData });

      const result = await getAdminReviews(token);

      expect(result).toEqual(pageData);
      expect(api.apiGet).toHaveBeenCalledTimes(1);
      expect(api.apiGet).toHaveBeenCalledWith('/admin/reviews?page=0&pageSize=10', { authToken: token });
    });

    it('后端返回 success=false 时抛错', async () => {
      const token = 'tk';
      api.apiGet.mockResolvedValue({ success: false, message: 'Failed to fetch reviews' });

      await expect(getAdminReviews(token)).rejects.toThrow('Failed to fetch reviews');
      expect(api.apiGet).toHaveBeenCalledWith('/admin/reviews?page=0&pageSize=10', { authToken: token });
    });
  });

  describe('toggleReviewVisibility', () => {
    it('PUT 切换可见性并返回数据', async () => {
      const token = 'tk';
      const phoneId = 'p1';
      const reviewId = 'r1';
      const data = { id: reviewId, phoneId, isHidden: true };
      api.apiPut.mockResolvedValue({ success: true, data });

      const result = await toggleReviewVisibility(token, phoneId, reviewId);

      expect(result).toEqual(data);
      expect(api.apiPut).toHaveBeenCalledTimes(1);
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/reviews/${phoneId}/${reviewId}/toggle-visibility`, {}, { authToken: token });
    });

    it('失败时抛错', async () => {
      const token = 'tk';
      const phoneId = 'p1';
      const reviewId = 'r1';
      api.apiPut.mockResolvedValue({ success: false, message: 'Failed to toggle review visibility' });

      await expect(toggleReviewVisibility(token, phoneId, reviewId)).rejects.toThrow('Failed to toggle review visibility');
      expect(api.apiPut).toHaveBeenCalledWith(`/admin/reviews/${phoneId}/${reviewId}/toggle-visibility`, {}, { authToken: token });
    });
  });

  describe('deleteReview', () => {
    it('DELETE 成功时不抛错', async () => {
      const token = 'tk';
      const phoneId = 'p2';
      const reviewId = 'r2';
      api.apiDelete.mockResolvedValue({ success: true });

      await expect(deleteReview(token, phoneId, reviewId)).resolves.toBeUndefined();
      expect(api.apiDelete).toHaveBeenCalledTimes(1);
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/reviews/${phoneId}/${reviewId}`, { authToken: token });
    });

    it('success=false 时抛错', async () => {
      const token = 'tk';
      const phoneId = 'p2';
      const reviewId = 'r2';
      api.apiDelete.mockResolvedValue({ success: false, message: 'Failed to delete review' });

      await expect(deleteReview(token, phoneId, reviewId)).rejects.toThrow('Failed to delete review');
      expect(api.apiDelete).toHaveBeenCalledWith(`/admin/reviews/${phoneId}/${reviewId}`, { authToken: token });
    });
  });
});

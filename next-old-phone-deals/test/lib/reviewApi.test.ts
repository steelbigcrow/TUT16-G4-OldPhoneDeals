import { getReviewsBySeller } from '@/lib/reviewApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn()
}));

describe('reviewApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as { apiGet: jest.Mock };
  const token = 'tk';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches seller reviews', async () => {
    const reviews = [
      { id: 'r1', rating: 4, comment: 'Nice', userId: 'u1', userName: 'A', isHidden: false, createdAt: 'now', phoneId: 'p1', phoneTitle: 'Phone' }
    ];
    api.apiGet.mockResolvedValue({ success: true, data: reviews });

    const result = await getReviewsBySeller(token);

    expect(api.apiGet).toHaveBeenCalledWith('/phones/reviews/by-seller', {
      authToken: token
    });
    expect(result).toEqual(reviews);
  });

  it('throws when api reports failure', async () => {
    api.apiGet.mockResolvedValue({ success: false, message: 'failed' });

    await expect(getReviewsBySeller(token)).rejects.toThrow('failed');
  });
});

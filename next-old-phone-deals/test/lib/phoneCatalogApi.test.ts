import { fetchSpecialPhones, searchPhones, __internal } from '@/lib/phoneCatalogApi';
import * as api from '@/lib/apiClient';
import type { CatalogPhone } from '@/types/phone';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn()
}));

const apiGetMock = api.apiGet as jest.MockedFunction<typeof api.apiGet>;

describe('phoneCatalogApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('normalizes phone payloads with mixed fields', () => {
    const normalized = __internal.normalizePhone({
      _id: '123',
      title: 'Vintage iPhone',
      brand: 'Apple',
      price: 199.99,
      stock: 2,
      image: '/static/img.png',
      reviews: [{ rating: 4 }, { rating: 5 }],
      seller: { firstName: 'Alice', lastName: 'Smith' }
    });

    expect(normalized).toMatchObject({
      id: '123',
      title: 'Vintage iPhone',
      brand: 'Apple',
      price: 199.99,
      stock: 2,
      imageUrl: 'http://localhost:3000/static/img.png',
      averageRating: 4.5,
      reviewCount: 2,
      sellerName: 'Alice Smith'
    });
  });

  it('fetchSpecialPhones returns normalized data', async () => {
    apiGetMock.mockResolvedValue({
      success: true,
      data: [
        {
          _id: 'p1',
          title: 'Seller Special',
          brand: 'Samsung',
          price: 299,
          stock: 5,
          imageUrl: 'http://cdn.example.com/p1.png'
        }
      ]
    });

    const result = await fetchSpecialPhones('soldOutSoon');

    expect(apiGetMock).toHaveBeenCalledWith('/phones?special=soldOutSoon');
    expect(result).toEqual([
      expect.objectContaining({
        id: 'p1',
        imageUrl: 'http://cdn.example.com/p1.png'
      })
    ]);
  });

  it('fetchSpecialPhones throws when backend response is invalid', async () => {
    apiGetMock.mockResolvedValue({ success: false, message: 'Bad request' });

    await expect(fetchSpecialPhones('bestSellers')).rejects.toThrow(
      'Bad request'
    );
  });

  it('searchPhones returns paginated catalog data', async () => {
    apiGetMock.mockResolvedValue({
      success: true,
      data: {
        phones: [
          {
            id: 'x1',
            title: 'Rare Nokia',
            brand: 'Nokia',
            price: 120,
            stock: 8
          }
        ],
        currentPage: 2,
        totalPages: 5,
        total: 40
      }
    });

    const result = await searchPhones({
      search: 'nokia',
      brand: 'Nokia',
      maxPrice: 500,
      page: 2,
      limit: 20
    });

    expect(apiGetMock).toHaveBeenCalledWith(
      '/phones?page=2&limit=20&search=nokia&brand=Nokia&maxPrice=500'
    );
    expect(result.currentPage).toBe(2);
    expect(result.totalPages).toBe(5);
    expect(result.total).toBe(40);
    expect(result.phones[0]).toMatchObject({
      id: 'x1',
      title: 'Rare Nokia'
    } as CatalogPhone);
  });

  it('searchPhones throws an error when backend payload is missing phones array', async () => {
    apiGetMock.mockResolvedValue({ success: false, message: 'No phones found' });

    await expect(searchPhones()).rejects.toThrow('No phones found');
  });

  it('buildSearchQuery ensures positive pagination defaults', () => {
    const query = __internal.buildSearchQuery({
      page: -5,
      limit: 0,
      maxPrice: -10
    });

    expect(query.get('page')).toBe('1');
    expect(query.get('limit')).toBe('10');
    expect(query.get('maxPrice')).toBe('0');
  });
});

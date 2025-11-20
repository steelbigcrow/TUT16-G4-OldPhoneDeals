import {
  createPhone,
  deletePhone,
  getSellerListings,
  togglePhoneDisabled,
  updatePhone
} from '@/lib/sellerListingsApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn(),
  apiPut: jest.fn(),
  apiDelete: jest.fn(),
  apiPatch: jest.fn()
}));

describe('sellerListingsApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPost: jest.Mock;
    apiPut: jest.Mock;
    apiDelete: jest.Mock;
  };

  const token = 'tk';
  const phone = {
    id: 'p1',
    title: 'Phone',
    brand: 'APPLE',
    price: 100,
    stock: 2,
    salesCount: 0,
    isDisabled: false,
    sellerId: 's1',
    sellerName: 'Seller',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    averageRating: 4.5,
    reviewCount: 3,
    reviews: []
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('loads seller listings with auth token', async () => {
    api.apiGet.mockResolvedValue({ success: true, data: [phone] });

    const result = await getSellerListings('seller-1', token);

    expect(api.apiGet).toHaveBeenCalledWith('/phones/by-seller/seller-1', {
      authToken: token
    });
    expect(result).toEqual([phone]);
  });

  it('throws when listing fetch fails', async () => {
    api.apiGet.mockResolvedValue({
      success: false,
      message: 'no listings'
    });

    await expect(getSellerListings('s1', token)).rejects.toThrow('no listings');
  });

  it('creates a phone listing', async () => {
    api.apiPost.mockResolvedValue({ success: true, data: phone });

    const payload = { title: 'Phone', brand: 'APPLE', price: 100, stock: 2 };
    const result = await createPhone(payload as any, token);

    expect(api.apiPost).toHaveBeenCalledWith('/phones', payload, {
      authToken: token
    });
    expect(result).toEqual(phone);
  });

  it('updates a phone listing', async () => {
    api.apiPut.mockResolvedValue({ success: true, data: phone });

    const result = await updatePhone('p1', { price: 120 } as any, token);

    expect(api.apiPut).toHaveBeenCalledWith('/phones/p1', { price: 120 }, {
      authToken: token
    });
    expect(result).toEqual(phone);
  });

  it('throws when create/update/delete fails', async () => {
    api.apiPost.mockResolvedValue({ success: false, message: 'create failed' });
    await expect(createPhone({} as any, token)).rejects.toThrow('create failed');

    api.apiPut.mockResolvedValue({ success: false, message: 'update failed' });
    await expect(updatePhone('p1', {} as any, token)).rejects.toThrow('update failed');

    api.apiDelete.mockResolvedValue({ success: false, message: 'delete failed' });
    await expect(deletePhone('p2', token)).rejects.toThrow('delete failed');
  });

  it('deletes a phone listing', async () => {
    api.apiDelete.mockResolvedValue({ success: true });

    await expect(deletePhone('p3', token)).resolves.toBeUndefined();
    expect(api.apiDelete).toHaveBeenCalledWith('/phones/p3', {
      authToken: token
    });
  });

  it('toggles disabled state', async () => {
    api.apiPut.mockResolvedValue({ success: true });

    await expect(togglePhoneDisabled('p4', true, token)).resolves.toBeUndefined();
    expect(api.apiPut).toHaveBeenCalledWith(
      '/phones/p4/disable',
      { isDisabled: true },
      { authToken: token }
    );
  });

  it('throws when toggle disabled fails', async () => {
    api.apiPut.mockResolvedValue({ success: false, message: 'toggle failed' });

    await expect(
      togglePhoneDisabled('p4', false, token)
    ).rejects.toThrow('toggle failed');
  });
});

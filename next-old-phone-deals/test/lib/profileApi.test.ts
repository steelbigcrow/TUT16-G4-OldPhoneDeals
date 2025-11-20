import { changePassword, getProfile, updateProfile } from '@/lib/profileApi';

jest.mock('@/lib/apiClient', () => ({
  apiGet: jest.fn(),
  apiPut: jest.fn()
}));

describe('profileApi', () => {
  const api = jest.requireMock('@/lib/apiClient') as {
    apiGet: jest.Mock;
    apiPut: jest.Mock;
  };
  const token = 'tk';
  const profile = {
    id: 'u1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    isVerified: true,
    isDisabled: false,
    createdAt: new Date().toISOString(),
    lastLoginAt: new Date().toISOString()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches profile by user id', async () => {
    api.apiGet.mockResolvedValue({ success: true, data: profile });

    const result = await getProfile('u1', token);

    expect(api.apiGet).toHaveBeenCalledWith('/profile/u1', { authToken: token });
    expect(result).toEqual(profile);
  });

  it('updates profile information', async () => {
    api.apiPut.mockResolvedValue({ success: true, data: profile });

    const result = await updateProfile('u1', { firstName: 'Jane', lastName: 'Smith' }, token);

    expect(api.apiPut).toHaveBeenCalledWith(
      '/profile/u1',
      { firstName: 'Jane', lastName: 'Smith' },
      { authToken: token }
    );
    expect(result).toEqual(profile);
  });

  it('changes password and throws on failure', async () => {
    api.apiPut.mockResolvedValueOnce({ success: true });
    await expect(
      changePassword('u1', { currentPassword: 'old', newPassword: 'new' }, token)
    ).resolves.toBeUndefined();
    expect(api.apiPut).toHaveBeenCalledWith(
      '/profile/u1/change-password',
      { currentPassword: 'old', newPassword: 'new' },
      { authToken: token }
    );

    api.apiPut.mockResolvedValueOnce({ success: false, message: 'change failed' });
    await expect(
      changePassword('u1', { currentPassword: 'old', newPassword: 'new' }, token)
    ).rejects.toThrow('change failed');
  });

  it('throws when fetching or updating fails', async () => {
    api.apiGet.mockResolvedValue({ success: false, message: 'no profile' });
    await expect(getProfile('u1', token)).rejects.toThrow('no profile');

    api.apiPut.mockResolvedValue({ success: false, message: 'update failed' });
    await expect(updateProfile('u1', {} as any, token)).rejects.toThrow('update failed');
  });
});

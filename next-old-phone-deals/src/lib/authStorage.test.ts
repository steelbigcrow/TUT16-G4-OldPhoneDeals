import {
  getUserToken,
  setUserToken,
  clearUserToken,
  getAdminToken,
  setAdminToken,
  clearAdminToken,
  clearAllTokens,
  getActiveToken,
} from './authStorage';

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores and retrieves user token', () => {
    expect(getUserToken()).toBeNull();

    setUserToken('user-token-123');
    expect(localStorage.getItem('opd_user_token')).toBe('user-token-123');
    expect(getUserToken()).toBe('user-token-123');

    clearUserToken();
    expect(localStorage.getItem('opd_user_token')).toBeNull();
    expect(getUserToken()).toBeNull();
  });

  it('stores and retrieves admin token', () => {
    expect(getAdminToken()).toBeNull();

    setAdminToken('admin-token-456');
    expect(localStorage.getItem('opd_admin_token')).toBe('admin-token-456');
    expect(getAdminToken()).toBe('admin-token-456');

    clearAdminToken();
    expect(localStorage.getItem('opd_admin_token')).toBeNull();
    expect(getAdminToken()).toBeNull();
  });

  it('clearAllTokens removes both user and admin tokens', () => {
    setUserToken('user-token');
    setAdminToken('admin-token');

    expect(getUserToken()).toBe('user-token');
    expect(getAdminToken()).toBe('admin-token');

    clearAllTokens();

    expect(getUserToken()).toBeNull();
    expect(getAdminToken()).toBeNull();
  });

  it('getActiveToken prefers admin token over user token', () => {
    expect(getActiveToken()).toBeNull();

    setUserToken('user-token');
    expect(getActiveToken()).toBe('user-token');

    setAdminToken('admin-token');
    expect(getActiveToken()).toBe('admin-token');

    clearAdminToken();
    expect(getActiveToken()).toBe('user-token');
  });
});
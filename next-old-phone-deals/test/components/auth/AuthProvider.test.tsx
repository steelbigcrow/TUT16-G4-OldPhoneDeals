import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '@/components/auth/AuthProvider';
import type { User, Admin } from '@/types/auth';

const pushMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: jest.fn(),
    prefetch: jest.fn(),
  }),
}));

const apiGetMock = jest.fn();

jest.mock('@/lib/apiClient', () => ({
  apiGet: (...args: unknown[]) => apiGetMock(...args),
}));

jest.mock('@/lib/authStorage', () => {
  return {
    getUserToken: jest.fn(),
    getAdminToken: jest.fn(),
    setUserToken: jest.fn(),
    setAdminToken: jest.fn(),
    clearUserToken: jest.fn(),
    clearAdminToken: jest.fn(),
    clearAllTokens: jest.fn(),
  };
});

describe('AuthProvider', () => {
  const authStorage = jest.requireMock('@/lib/authStorage') as {
    getUserToken: jest.Mock;
    getAdminToken: jest.Mock;
    setUserToken: jest.Mock;
    setAdminToken: jest.Mock;
    clearUserToken: jest.Mock;
    clearAdminToken: jest.Mock;
    clearAllTokens: jest.Mock;
  };

  beforeEach(() => {
    jest.clearAllMocks();
    authStorage.getUserToken.mockReturnValue(null);
    authStorage.getAdminToken.mockReturnValue(null);
    apiGetMock.mockReset();
  });

  const Consumer: React.FC = () => {
    const ctx = useAuth();
    return (
      <div>
        <div data-testid="user-email">{ctx.user?.email ?? 'none'}</div>
        <div data-testid="admin-email">{ctx.admin?.email ?? 'none'}</div>
        <div data-testid="loading">{ctx.isLoading ? 'loading' : 'ready'}</div>
      </div>
    );
  };

  it('initializes with null user and admin when no tokens', async () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    );

    await waitFor(() =>
      expect(screen.getByTestId('loading').textContent).toBe('ready'),
    );

    expect(screen.getByTestId('user-email').textContent).toBe('none');
    expect(screen.getByTestId('admin-email').textContent).toBe('none');
    expect(authStorage.getUserToken).toHaveBeenCalled();
    expect(authStorage.getAdminToken).toHaveBeenCalled();
  });

  it('loginUser updates context and calls storage helpers', async () => {
    let ctxRef: any;
    const Capture: React.FC = () => {
      const ctx = useAuth();
      ctxRef = ctx;
      return (
        <div>
          <div data-testid="user-email">{ctx.user?.email ?? 'none'}</div>
          <div data-testid="admin-email">{ctx.admin?.email ?? 'none'}</div>
        </div>
      );
    };

    render(
      <AuthProvider>
        <Capture />
      </AuthProvider>,
    );

    const user: User = {
      id: 'u1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'user@example.com',
      isVerified: true,
      isDisabled: false,
      createdAt: new Date().toISOString(),
      lastLoginAt: new Date().toISOString(),
    };

    await act(async () => {
      ctxRef.loginUser('user-token', user);
    });

    expect(authStorage.setUserToken).toHaveBeenCalledWith('user-token');
    expect(authStorage.clearAdminToken).toHaveBeenCalled();

    await waitFor(() =>
      expect(screen.getByTestId('user-email').textContent).toBe(
        'user@example.com',
      ),
    );
    expect(screen.getByTestId('admin-email').textContent).toBe('none');
  });

  it('loginAdmin updates admin context and clears user state', async () => {
    let ctxRef: any;
    const Capture: React.FC = () => {
      const ctx = useAuth();
      ctxRef = ctx;
      return (
        <div>
          <div data-testid="user-email">{ctx.user?.email ?? 'none'}</div>
          <div data-testid="admin-email">{ctx.admin?.email ?? 'none'}</div>
        </div>
      );
    };

    render(
      <AuthProvider>
        <Capture />
      </AuthProvider>,
    );

    const admin: Admin = {
      id: 'a1',
      firstName: 'Admin',
      lastName: 'User',
      email: 'admin@example.com',
      role: 'ADMIN',
      createdAt: new Date().toISOString(),
      lastLoginAt: new Date().toISOString(),
    };

    await act(async () => {
      ctxRef.loginAdmin('admin-token', admin);
    });

    expect(authStorage.setAdminToken).toHaveBeenCalledWith('admin-token');
    expect(authStorage.clearUserToken).toHaveBeenCalled();

    await waitFor(() =>
      expect(screen.getByTestId('admin-email').textContent).toBe(
        'admin@example.com',
      ),
    );
    expect(screen.getByTestId('user-email').textContent).toBe('none');
  });

  it('logout clears tokens, resets state and navigates home', async () => {
    let ctxRef: any;
    const Capture: React.FC = () => {
      const ctx = useAuth();
      ctxRef = ctx;
      return (
        <div>
          <div data-testid="user-email">{ctx.user?.email ?? 'none'}</div>
          <div data-testid="admin-email">{ctx.admin?.email ?? 'none'}</div>
        </div>
      );
    };

    render(
      <AuthProvider>
        <Capture />
      </AuthProvider>,
    );

    await act(async () => {
      ctxRef.setUser({
        id: 'u1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'user@example.com',
        isVerified: true,
        isDisabled: false,
        createdAt: new Date().toISOString(),
      });
      ctxRef.setAdmin({
        id: 'a1',
        firstName: 'Admin',
        lastName: 'User',
        email: 'admin@example.com',
        role: 'ADMIN',
        createdAt: new Date().toISOString(),
      });
    });

    await act(async () => {
      ctxRef.logout();
    });

    expect(authStorage.clearAllTokens).toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith('/');

    await waitFor(() =>
      expect(screen.getByTestId('user-email').textContent).toBe('none'),
    );
    expect(screen.getByTestId('admin-email').textContent).toBe('none');
  });
});

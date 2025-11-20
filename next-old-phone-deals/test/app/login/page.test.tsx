import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginPage from '@/app/login/page';
import type { ApiResponse, LoginResponse } from '@/types/auth';

const pushMock = jest.fn();

let mockSearchParams: Record<string, string | undefined> = {};

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: jest.fn(),
    prefetch: jest.fn(),
  }),
  useSearchParams: () => ({
    get: (key: string) => mockSearchParams[key] ?? null,
  }),
}));

const loginUserMock = jest.fn();
const loginAdminMock = jest.fn();

type MockAuth = {
  user: any;
  admin: any;
  loginUser: typeof loginUserMock;
  loginAdmin: typeof loginAdminMock;
};

let mockAuth: MockAuth = {
  user: null,
  admin: null,
  loginUser: loginUserMock,
  loginAdmin: loginAdminMock,
};

jest.mock('@/components/auth/AuthProvider', () => ({
  useAuth: () => mockAuth,
}));

const apiPostMock = jest.fn();

jest.mock('@/lib/apiClient', () => ({
  apiPost: (...args: unknown[]) => apiPostMock(...args),
}));

describe('LoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearchParams = {};
    mockAuth = {
      user: null,
      admin: null,
      loginUser: loginUserMock,
      loginAdmin: loginAdminMock,
    };
  });

  it('renders email/password inputs and mode tabs', () => {
    render(<LoginPage />);

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /user login/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /admin login/i })).toBeInTheDocument();
  });

  it('switches between user and admin tabs', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    const submitButton = screen.getByRole('button', { name: /login as user/i });
    expect(submitButton).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /admin login/i }));

    expect(
      screen.getByRole('button', { name: /login as admin/i }),
    ).toBeInTheDocument();
  });

  it('submits user login and redirects with redirect param', async () => {
    const user = userEvent.setup();
    mockSearchParams = { redirect: '/checkout' };

    const apiResponse: ApiResponse<LoginResponse> = {
      success: true,
      message: 'ok',
      data: {
        token: 'user-token',
        user: {
          id: 'u1',
          firstName: 'John',
          lastName: 'Doe',
          email: 'user@example.com',
          isVerified: true,
          isDisabled: false,
          createdAt: new Date().toISOString(),
          lastLoginAt: new Date().toISOString(),
        },
      },
    };

    apiPostMock.mockResolvedValue(apiResponse);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/email/i), 'user@example.com');
    await user.type(screen.getByLabelText(/password/i), 'Password123');

    await user.click(
      screen.getByRole('button', { name: /login as user/i }),
    );

    await waitFor(() => {
      expect(loginUserMock).toHaveBeenCalledWith(
        'user-token',
        expect.objectContaining({ email: 'user@example.com' }),
      );
    });

    expect(apiPostMock).toHaveBeenCalledWith(
      '/auth/login',
      { email: 'user@example.com', password: 'Password123' },
    );
    expect(pushMock).toHaveBeenCalledWith('/checkout');
  });

  it('submits admin login and redirects to /admin when mode=admin from url', async () => {
    const user = userEvent.setup();
    mockSearchParams = { mode: 'admin' };

    const apiResponse: ApiResponse<LoginResponse> = {
      success: true,
      message: 'ok',
      data: {
        token: 'admin-token',
        admin: {
          id: 'a1',
          firstName: 'Admin',
          lastName: 'User',
          email: 'admin@example.com',
          role: 'ADMIN',
          createdAt: new Date().toISOString(),
          lastLoginAt: new Date().toISOString(),
        },
      },
    };

    apiPostMock.mockResolvedValue(apiResponse);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/email/i), 'admin@example.com');
    await user.type(screen.getByLabelText(/password/i), 'AdminPass123');

    await user.click(
      screen.getByRole('button', { name: /login as admin/i }),
    );

    await waitFor(() => {
      expect(loginAdminMock).toHaveBeenCalledWith(
        'admin-token',
        expect.objectContaining({ email: 'admin@example.com' }),
      );
    });

    expect(apiPostMock).toHaveBeenCalledWith(
      '/admin/login',
      { email: 'admin@example.com', password: 'AdminPass123' },
    );
    expect(pushMock).toHaveBeenCalledWith('/admin');
  });
});

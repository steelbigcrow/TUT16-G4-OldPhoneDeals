import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Header from '@/components/common/Header';

const pushMock = jest.fn();
let searchParams = new URLSearchParams();

type MockAuth = {
  user: any;
  admin: any;
  logout: jest.Mock;
};

let mockAuth: MockAuth = {
  user: null,
  admin: null,
  logout: jest.fn()
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: jest.fn(),
    prefetch: jest.fn()
  }),
  useSearchParams: () => searchParams
}));

jest.mock('@/components/auth/AuthProvider', () => ({
  useAuth: () => mockAuth
}));

function setSearchParams(params: Record<string, string> | string) {
  if (typeof params === 'string') {
    searchParams = new URLSearchParams(params);
    return;
  }
  const entries = Object.entries(params);
  searchParams = new URLSearchParams(entries);
}

describe('Header', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setSearchParams('');
    mockAuth = { user: null, admin: null, logout: jest.fn() };
  });

  it('prefills filters from query params and submits composed search url', async () => {
    setSearchParams({ q: 'iphone', brand: 'Apple', maxPrice: '750' });
    const user = userEvent.setup();

    render(<Header />);

    await waitFor(() => {
      expect(screen.getByLabelText(/Search phones/i)).toHaveValue('iphone');
      expect(screen.getByLabelText(/Brand/i)).toHaveValue('Apple');
    });
    expect(screen.getByLabelText(/Max price/i)).toHaveValue('750');

    await user.clear(screen.getByLabelText(/Search phones/i));
    await user.type(screen.getByLabelText(/Search phones/i), 'pixel');
    await user.selectOptions(screen.getByLabelText(/Brand/i), 'Samsung');
    fireEvent.change(screen.getByLabelText(/Max price/i), { target: { value: '1500' } });

    await user.click(screen.getByRole('button', { name: /Search/i }));

    expect(pushMock).toHaveBeenCalled();
    const url = pushMock.mock.calls[0][0] as string;
    const params = new URLSearchParams(url.replace('/search?', ''));
    expect(params.get('q')).toBe('pixel');
    expect(params.get('brand')).toBe('Samsung');
    expect(params.get('maxPrice')).toBe('1500');
    expect(params.get('sort')).toBe('default');
    expect(params.get('page')).toBe('1');
    expect(params.get('limit')).toBe('10');
  });

  it('redirects guests to login when accessing wishlist or checkout', async () => {
    const user = userEvent.setup();
    render(<Header />);

    await user.click(screen.getByRole('button', { name: /Wishlist/i }));
    await user.click(screen.getByRole('button', { name: /Checkout/i }));

    expect(pushMock).toHaveBeenCalledWith('/login?redirect=%2Fwishlist');
    expect(pushMock).toHaveBeenCalledWith('/login?redirect=%2Fcheckout');
  });

  it('navigates for authenticated users and supports logout', async () => {
    const user = userEvent.setup();
    mockAuth = {
      user: { firstName: 'Jane' },
      admin: null,
      logout: jest.fn()
    };

    render(<Header />);

    await user.click(screen.getByRole('button', { name: /Wishlist/i }));
    await user.click(screen.getByRole('button', { name: /Checkout/i }));
    await user.click(screen.getByRole('button', { name: /Logout/i }));

    expect(pushMock).toHaveBeenCalledWith('/wishlist');
    expect(pushMock).toHaveBeenCalledWith('/checkout');
    expect(mockAuth.logout).toHaveBeenCalled();
    expect(screen.getByText(/Hello, Jane/)).toBeInTheDocument();
    expect(screen.queryByText(/Login/i)).not.toBeInTheDocument();
  });

  it('shows admin entry when admin is present', () => {
    mockAuth = {
      user: null,
      admin: { firstName: 'Root' },
      logout: jest.fn()
    };

    render(<Header />);

    expect(screen.getByText(/Admin: Root/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Admin/i })).toBeInTheDocument();
    expect(screen.queryByText(/Register/i)).not.toBeInTheDocument();
  });
});

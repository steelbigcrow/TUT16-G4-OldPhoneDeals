import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchPage from '@/app/search/page';
import type { CatalogPhone } from '@/types/phone';

const pushMock = jest.fn();
let searchParamsString = '';

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: jest.fn(),
    prefetch: jest.fn()
  }),
  useSearchParams: () => new URLSearchParams(searchParamsString)
}));

const searchPhonesMock = jest.fn();

jest.mock('@/lib/phoneCatalogApi', () => ({
  searchPhones: (...args: unknown[]) => searchPhonesMock(...args)
}));

function setSearchParams(params: Record<string, string> | string) {
  if (typeof params === 'string') {
    searchParamsString = params;
    return;
  }
  const entries = Object.entries(params).filter(([, value]) => value !== '');
  searchParamsString = new URLSearchParams(entries).toString();
}

describe('SearchPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setSearchParams('');
  });

  it('renders server-side paginated results when sort=default', async () => {
    const phones: CatalogPhone[] = [
      {
        id: 'p1',
        title: 'Alpha Phone',
        brand: 'Apple',
        price: 199,
        stock: 4
      },
      {
        id: 'p2',
        title: 'Beta Phone',
        brand: 'Samsung',
        price: 299,
        stock: 2
      }
    ];

    searchPhonesMock.mockResolvedValue({
      phones,
      currentPage: 1,
      totalPages: 3,
      total: 25
    });

    render(<SearchPage />);

    await waitFor(() => {
      expect(screen.getByText('Alpha Phone')).toBeInTheDocument();
      expect(screen.getByText('Beta Phone')).toBeInTheDocument();
    });

    expect(searchPhonesMock).toHaveBeenCalledWith({
      search: undefined,
      brand: undefined,
      maxPrice: null,
      page: 1,
      limit: 10
    });
  });

  it('applies sorting client-side and pushes router updates', async () => {
    setSearchParams({ sort: 'price_desc', page: '3', limit: '5' });

    const phones: CatalogPhone[] = [
      {
        id: 'p1',
        title: 'Cheaper Phone',
        brand: 'Apple',
        price: 100,
        stock: 10
      },
      {
        id: 'p2',
        title: 'Expensive Phone',
        brand: 'Apple',
        price: 900,
        stock: 1
      }
    ];

    searchPhonesMock.mockResolvedValue({
      phones,
      currentPage: 1,
      totalPages: 1,
      total: 2
    });

    render(<SearchPage />);

    await waitFor(() => {
      expect(searchPhonesMock).toHaveBeenCalledWith({
        search: undefined,
        brand: undefined,
        maxPrice: null,
        page: 1,
        limit: 1000
      });
    });

    const select = await screen.findByLabelText(/Sort by/i);
    await userEvent.selectOptions(select, 'price_asc');

    expect(pushMock).toHaveBeenCalledWith('/search?sort=price_asc&page=1&limit=5');
  });

  it('shows error state and retries fetching', async () => {
    searchPhonesMock
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce({
        phones: [],
        currentPage: 1,
        totalPages: 1,
        total: 0
      });

    render(<SearchPage />);

    expect(await screen.findByText('Network error')).toBeInTheDocument();

    const retryButton = screen.getByRole('button', { name: /Try again/i });
    await userEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText(/No phones matched/i)).toBeInTheDocument();
    });

    expect(searchPhonesMock).toHaveBeenCalledTimes(2);
  });

  it('clears filters via router navigation', async () => {
    setSearchParams({ q: 'iphone', brand: 'Apple', maxPrice: '500' });

    searchPhonesMock.mockResolvedValue({
      phones: [],
      currentPage: 1,
      totalPages: 1,
      total: 0
    });

    render(<SearchPage />);

    await waitFor(() => {
      expect(searchPhonesMock).toHaveBeenCalled();
    });

    const clearButton = await screen.findByRole('button', {
      name: /Clear filters/i
    });
    await userEvent.click(clearButton);

    expect(pushMock).toHaveBeenCalledWith('/search');
  });
});

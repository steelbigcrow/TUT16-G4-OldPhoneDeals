'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import PhoneCard from '@/components/catalog/PhoneCard';
import { searchPhones } from '@/lib/phoneCatalogApi';
import type { CatalogPhone } from '@/types/phone';

type SortingOption =
  | 'default'
  | 'price_asc'
  | 'price_desc'
  | 'stock_asc'
  | 'stock_desc';

type ParsedFilters = {
  query: string;
  brand: string;
  maxPrice: number | null;
  sort: SortingOption;
  page: number;
  limit: number;
};

const SORTING_OPTIONS: SortingOption[] = [
  'default',
  'price_asc',
  'price_desc',
  'stock_asc',
  'stock_desc'
];

function parsePositiveInt(
  value: string | null,
  fallback: number,
  max = 1000
): number {
  const parsed = Number(value);
  if (Number.isFinite(parsed) && parsed > 0) {
    return Math.min(Math.floor(parsed), max);
  }
  return fallback;
}

function parseFilters(params: URLSearchParams): ParsedFilters {
  const maxPriceParam = params.get('maxPrice');
  const parsedMaxPrice = maxPriceParam ? Number(maxPriceParam) : null;
  const rawSort = params.get('sort') as SortingOption | null;
  const sort = SORTING_OPTIONS.includes(rawSort ?? 'default')
    ? rawSort ?? 'default'
    : 'default';

  return {
    query: params.get('q') ?? '',
    brand: params.get('brand') ?? '',
    maxPrice:
      typeof parsedMaxPrice === 'number' && !Number.isNaN(parsedMaxPrice)
        ? parsedMaxPrice
        : null,
    sort,
    page: parsePositiveInt(params.get('page'), 1),
    limit: parsePositiveInt(params.get('limit'), 10, 50)
  };
}

function sortPhones(phones: CatalogPhone[], sort: SortingOption) {
  if (sort === 'default') {
    return phones;
  }

  const sorted = [...phones];
  switch (sort) {
    case 'price_asc':
      sorted.sort((a, b) => a.price - b.price);
      break;
    case 'price_desc':
      sorted.sort((a, b) => b.price - a.price);
      break;
    case 'stock_asc':
      sorted.sort((a, b) => a.stock - b.stock);
      break;
    case 'stock_desc':
      sorted.sort((a, b) => b.stock - a.stock);
      break;
  }
  return sorted;
}

function paginate(
  phones: CatalogPhone[],
  page: number,
  limit: number
): CatalogPhone[] {
  const safeLimit = Math.max(1, limit);
  const start = (Math.max(1, page) - 1) * safeLimit;
  return phones.slice(start, start + safeLimit);
}

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onChange: (page: number) => void;
};

function Pagination(props: PaginationProps): JSX.Element | null {
  const { currentPage, totalPages, onChange } = props;

  if (totalPages <= 1) {
    return null;
  }

  const pages = Array.from({ length: totalPages }, (_, index) => index + 1);

  return (
    <div className="flex flex-wrap items-center justify-center gap-2">
      <button
        type="button"
        onClick={() => onChange(Math.max(currentPage - 1, 1))}
        disabled={currentPage === 1}
        className="rounded-lg border border-gray-300 px-3 py-1 text-sm text-gray-600 disabled:opacity-50"
      >
        Previous
      </button>
      {pages.map((pageNumber) => (
        <button
          key={pageNumber}
          type="button"
          onClick={() => onChange(pageNumber)}
          className={`rounded-lg px-3 py-1 text-sm ${
            pageNumber === currentPage
              ? 'bg-blue-600 text-white'
              : 'border border-gray-200 text-gray-700 hover:border-blue-300'
          }`}
        >
          {pageNumber}
        </button>
      ))}
      <button
        type="button"
        onClick={() => onChange(Math.min(currentPage + 1, totalPages))}
        disabled={currentPage === totalPages}
        className="rounded-lg border border-gray-300 px-3 py-1 text-sm text-gray-600 disabled:opacity-50"
      >
        Next
      </button>
    </div>
  );
}

export default function SearchPage(): JSX.Element {
  const router = useRouter();
  const searchParams = useSearchParams();
  const paramsSnapshot = searchParams.toString();

  const filters = useMemo(
    () => parseFilters(searchParams),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [paramsSnapshot]
  );

  const { query, brand, maxPrice, sort, page, limit } = filters;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dataset, setDataset] = useState<CatalogPhone[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalResults, setTotalResults] = useState(0);
  const [currentPage, setCurrentPage] = useState(page);
  const [mode, setMode] = useState<'server' | 'client'>('server');
  const [reloadFlag, setReloadFlag] = useState(0);

  useEffect(() => {
    setCurrentPage(page);
  }, [page]);

  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      setLoading(true);
      setError(null);

      try {
        if (sort === 'default') {
          setMode('server');
          const response = await searchPhones({
            search: query || undefined,
            brand: brand || undefined,
            maxPrice,
            page,
            limit
          });

          if (cancelled) {
            return;
          }

          setDataset(response.phones);
          setTotalPages(Math.max(1, response.totalPages));
          setTotalResults(response.total);
          setCurrentPage(response.currentPage);
        } else {
          setMode('client');
          const response = await searchPhones({
            search: query || undefined,
            brand: brand || undefined,
            maxPrice,
            page: 1,
            limit: 1000
          });

          if (cancelled) {
            return;
          }

          const sorted = sortPhones(response.phones, sort);
          const datasetTotal = sorted.length;
          const nextTotalPages = Math.max(
            1,
            Math.ceil(datasetTotal / limit)
          );
          const safePage = Math.min(Math.max(page, 1), nextTotalPages);

          setDataset(sorted);
          setTotalPages(nextTotalPages);
          setTotalResults(response.total ?? datasetTotal);
          setCurrentPage(safePage);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Failed to load search results'
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, [
    brand,
    limit,
    maxPrice,
    page,
    query,
    reloadFlag,
    sort
  ]);

  const visiblePhones =
    mode === 'client' ? paginate(dataset, currentPage, limit) : dataset;

  const hasFilters = Boolean(
    (query && query.trim().length > 0) || brand || maxPrice !== null
  );

  const startIndex =
    totalResults === 0 ? 0 : (currentPage - 1) * limit + 1;
  const endIndex =
    totalResults === 0
      ? 0
      : Math.min(startIndex + visiblePhones.length - 1, totalResults);

  const pushWithParams = (params: URLSearchParams) => {
    const next = params.toString();
    router.push(next ? `/search?${next}` : '/search');
  };

  const handlePageChange = (nextPage: number) => {
    const params = new URLSearchParams(paramsSnapshot);
    params.set('page', String(nextPage));
    pushWithParams(params);
  };

  const handleSortingChange = (
    event: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const nextValue = event.target.value as SortingOption;
    const params = new URLSearchParams(paramsSnapshot);
    if (nextValue === 'default') {
      params.delete('sort');
    } else {
      params.set('sort', nextValue);
    }
    params.set('page', '1');
    pushWithParams(params);
  };

  const handleClearFilters = () => {
    const params = new URLSearchParams(paramsSnapshot);
    ['q', 'brand', 'maxPrice', 'sort', 'page'].forEach((key) =>
      params.delete(key)
    );
    pushWithParams(params);
  };

  const handleRetry = () => {
    setReloadFlag((flag) => flag + 1);
  };

  const handleBackHome = () => {
    router.push('/');
  };

  return (
    <section className="space-y-6">
      <div className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">
              {query ? `Search results: “${query}”` : 'All phones'}
            </h1>
            <div className="mt-2 space-x-3 text-sm text-gray-600">
              {brand && (
                <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1">
                  Brand: {brand}
                </span>
              )}
              {maxPrice !== null && (
                <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1">
                  Max Price: ${maxPrice}
                </span>
              )}
              {hasFilters && (
                <button
                  type="button"
                  onClick={handleClearFilters}
                  className="inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-700"
                >
                  Clear filters
                </button>
              )}
            </div>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div>
              <label
                htmlFor="sort"
                className="block text-sm font-medium text-gray-700"
              >
                Sort by
              </label>
              <select
                id="sort"
                value={sort}
                onChange={handleSortingChange}
                className="mt-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
              >
                <option value="default">Default</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="stock_asc">Stock: Low to High</option>
                <option value="stock_desc">Stock: High to Low</option>
              </select>
            </div>

            <button
              type="button"
              onClick={handleBackHome}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              Back to home
            </button>
          </div>
        </div>

        <div className="mt-4 text-sm text-gray-500">
          {totalResults > 0
            ? `Showing ${startIndex}-${endIndex} of ${totalResults} results`
            : 'No results yet'}
        </div>
      </div>

      {loading ? (
        <div className="rounded-3xl border border-gray-200 bg-white p-8 text-center text-gray-500 shadow-sm">
          Loading phones...
        </div>
      ) : error ? (
        <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center text-red-700 shadow-sm space-y-4">
          <p>{error}</p>
          <button
            type="button"
            onClick={handleRetry}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
          >
            Try again
          </button>
        </div>
      ) : visiblePhones.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-gray-200 bg-white p-10 text-center text-gray-600 shadow-sm">
          No phones matched your filters. Adjust the filters or try another
          keyword.
        </div>
      ) : (
        <>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {visiblePhones.map((phone) => (
              <PhoneCard key={phone.id} phone={phone} />
            ))}
          </div>
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onChange={handlePageChange}
          />
        </>
      )}
    </section>
  );
}

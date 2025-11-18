'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';

const BRAND_OPTIONS = [
  'Apple',
  'Samsung',
  'BlackBerry',
  'Sony',
  'Huawei',
  'HTC',
  'Nokia',
  'LG',
  'Motorola'
];

const DEFAULT_MAX_PRICE = 1000;

const Header: React.FC = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, admin, logout } = useAuth();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBrand, setSelectedBrand] = useState('');
  const [maxPrice, setMaxPrice] = useState(DEFAULT_MAX_PRICE);

  useEffect(() => {
    setSearchQuery(searchParams.get('q') ?? '');
    setSelectedBrand(searchParams.get('brand') ?? '');

    const maxPriceParam = searchParams.get('maxPrice');
    setMaxPrice(
      maxPriceParam ? Number(maxPriceParam) || DEFAULT_MAX_PRICE : DEFAULT_MAX_PRICE
    );
  }, [searchParams]);

  const buildSearchQuery = () => {
    const params = new URLSearchParams();
    if (searchQuery.trim()) {
      params.set('q', searchQuery.trim());
    }
    if (selectedBrand) {
      params.set('brand', selectedBrand);
    }
    params.set('maxPrice', String(maxPrice));
    params.set('sort', 'default');
    params.set('page', '1');
    params.set('limit', '10');
    return params.toString();
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const query = buildSearchQuery();
    router.push(query ? `/search?${query}` : '/search');
  };

  const redirectToLogin = (path: string) => {
    router.push(`/login?redirect=${encodeURIComponent(path)}`);
  };

  const handleWishlistClick = () => {
    if (user) {
      router.push('/wishlist');
    } else {
      redirectToLogin('/wishlist');
    }
  };

  const handleCheckoutClick = () => {
    if (user) {
      router.push('/checkout');
    } else {
      redirectToLogin('/checkout');
    }
  };

  return (
    <header className="sticky top-0 z-30 border-b border-gray-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-4">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <Link href="/" className="text-xl font-semibold text-gray-900">
            Old Phone Deals
          </Link>

          <div className="flex flex-wrap items-center gap-2 text-sm">
            <button
              type="button"
              onClick={handleWishlistClick}
              className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
            >
              Wishlist
            </button>
            <button
              type="button"
              onClick={handleCheckoutClick}
              className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
            >
              Checkout
            </button>
            {user && (
              <Link
                href="/user/profile"
                className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
              >
                Profile
              </Link>
            )}
            {admin && (
              <Link
                href="/admin"
                className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
              >
                Admin
              </Link>
            )}
            {!user && !admin && (
              <>
                <Link
                  href="/login"
                  className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
                >
                  Login
                </Link>
                <Link
                  href="/register"
                  className="rounded-full border border-gray-300 px-4 py-1 text-gray-700 hover:border-blue-400"
                >
                  Register
                </Link>
              </>
            )}
            {(user || admin) && (
              <>
                <span className="text-sm font-medium text-gray-900">
                  {admin
                    ? `Admin: ${admin.firstName}`
                    : `Hello, ${user?.firstName}`}
                </span>
                <button
                  type="button"
                  onClick={logout}
                  className="rounded-full bg-gray-100 px-4 py-1 text-sm font-medium text-gray-700 hover:bg-gray-200"
                >
                  Logout
                </button>
              </>
            )}
          </div>
        </div>

        <form
          onSubmit={handleSubmit}
          className="flex flex-col gap-3 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm md:flex-row md:items-end"
        >
          <div className="flex-1">
            <label
              htmlFor="header-search"
              className="block text-sm font-medium text-gray-700"
            >
              Search phones
            </label>
            <input
              id="header-search"
              type="text"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search phones..."
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>

          <div className="w-full md:w-48">
            <label
              htmlFor="header-brand"
              className="block text-sm font-medium text-gray-700"
            >
              Brand
            </label>
            <select
              id="header-brand"
              value={selectedBrand}
              onChange={(event) => setSelectedBrand(event.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="">All brands</option>
              {BRAND_OPTIONS.map((brand) => (
                <option key={brand} value={brand}>
                  {brand}
                </option>
              ))}
            </select>
          </div>

          <div className="w-full md:w-48">
            <label
              htmlFor="header-max-price"
              className="block text-sm font-medium text-gray-700"
            >
              Max price: ${maxPrice}
            </label>
            <input
              id="header-max-price"
              type="range"
              min={0}
              max={2000}
              step={10}
              value={maxPrice}
              onChange={(event) => setMaxPrice(Number(event.target.value))}
              className="mt-1 w-full"
            />
          </div>

          <div className="w-full md:w-auto">
            <button
              type="submit"
              className="w-full rounded-lg bg-blue-600 px-6 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              Search
            </button>
          </div>
        </form>
      </div>
    </header>
  );
};

export default Header;

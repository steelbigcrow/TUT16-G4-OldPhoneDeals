import React from 'react';
import Link from 'next/link';
import PhoneCard from '@/components/catalog/PhoneCard';
import { fetchSpecialPhones } from '@/lib/phoneCatalogApi';
import type { CatalogPhone } from '@/types/phone';

type SectionProps = {
  title: string;
  subtitle: string;
  phones: CatalogPhone[];
};

async function loadFeaturedPhones() {
  try {
    const [soldOutSoon, bestSellers] = await Promise.all([
      fetchSpecialPhones('soldOutSoon'),
      fetchSpecialPhones('bestSellers')
    ]);

    return {
      soldOutSoon,
      bestSellers,
      error: null as string | null
    };
  } catch (error) {
    console.error('Failed to load featured phones', error);
    return {
      soldOutSoon: [] as CatalogPhone[],
      bestSellers: [] as CatalogPhone[],
      error:
        error instanceof Error
          ? error.message
          : 'Failed to load the latest phone deals.'
    };
  }
}

function FeaturedSection(props: SectionProps): JSX.Element {
  const { title, subtitle, phones } = props;

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">{title}</h2>
          <p className="text-sm text-gray-500">{subtitle}</p>
        </div>
        <Link
          href="/search"
          className="inline-flex items-center text-sm font-semibold text-blue-600 hover:text-blue-700"
        >
          Explore all deals →
        </Link>
      </div>

      {phones.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-gray-300 bg-white p-6 text-center text-sm text-gray-600">
          No phones available in this section yet. Please check back soon!
        </div>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {phones.map((phone) => (
            <PhoneCard key={phone.id} phone={phone} />
          ))}
        </div>
      )}
    </section>
  );
}

export default async function HomePage(): Promise<JSX.Element> {
  const { soldOutSoon, bestSellers, error } = await loadFeaturedPhones();

  return (
    <section className="space-y-10">
      <div className="rounded-3xl bg-gradient-to-br from-blue-600 to-indigo-600 p-8 text-white shadow-lg">
        <p className="text-sm uppercase tracking-[0.2em] text-blue-100">
          Welcome to Old Phone Deals
        </p>
        <h1 className="mt-3 text-3xl font-bold md:text-4xl">
          Trade smarter. Buy pre-loved phones with confidence.
        </h1>
        <p className="mt-4 max-w-2xl text-base text-blue-100">
          Browse curated highlights that match the Angular user home experience:
          low-stock picks that might disappear soon and best sellers loved by
          reviewers.
        </p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link
            href="/search"
            className="inline-flex items-center justify-center rounded-full bg-white/10 px-5 py-2 text-sm font-semibold text-white ring-1 ring-white/40 backdrop-blur"
          >
            Start searching
          </Link>
          <Link
            href="/wishlist"
            className="inline-flex items-center justify-center rounded-full bg-white px-5 py-2 text-sm font-semibold text-blue-700"
          >
            View wishlist
          </Link>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <FeaturedSection
        title="Sold Out Soon"
        subtitle="Phones with limited stock remaining — grab them while you can."
        phones={soldOutSoon}
      />

      <FeaturedSection
        title="Best Sellers"
        subtitle="Top-rated phones with the strongest review signals from our community."
        phones={bestSellers}
      />
    </section>
  );
}

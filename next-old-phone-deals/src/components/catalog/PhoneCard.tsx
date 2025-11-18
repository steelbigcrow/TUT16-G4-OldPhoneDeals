import React from 'react';
import Link from 'next/link';
import type { CatalogPhone } from '@/types/phone';

type PhoneCardProps = {
  phone: CatalogPhone;
};

const formatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0
});

function getStockLabel(stock: number): { text: string; tone: string } {
  if (stock <= 0) {
    return { text: 'Out of stock', tone: 'text-red-600' };
  }

  if (stock > 0 && stock < 4) {
    return { text: `Only ${stock} left`, tone: 'text-orange-600' };
  }

  return { text: `${stock} in stock`, tone: 'text-gray-600' };
}

export default function PhoneCard(props: PhoneCardProps): JSX.Element {
  const { phone } = props;
  const rating =
    typeof phone.averageRating === 'number' ? phone.averageRating : undefined;
  const stockInfo = getStockLabel(phone.stock);

  return (
    <article className="flex h-full flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition hover:shadow-lg">
      <div className="flex h-48 items-center justify-center bg-gray-50">
        {phone.imageUrl ? (
          <img
            src={phone.imageUrl}
            alt={phone.title}
            className="h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="text-sm text-gray-500">No image available</div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-gray-500">
            {phone.brand}
          </p>
          <h3 className="line-clamp-2 text-lg font-semibold text-gray-900">
            {phone.title}
          </h3>
        </div>

        <div className="flex items-center justify-between text-sm text-gray-600">
          <span className="text-xl font-bold text-blue-600">
            {formatter.format(phone.price)}
          </span>
          {phone.reviewCount !== undefined && (
            <span className="text-xs text-gray-500">
              ⭐ {rating?.toFixed(1) ?? '0.0'} ({phone.reviewCount})
            </span>
          )}
        </div>

        <p className={`text-xs font-medium ${stockInfo.tone}`}>
          {stockInfo.text}
        </p>

        {phone.sellerName && (
          <p className="text-xs text-gray-500">Seller: {phone.sellerName}</p>
        )}

        <div className="mt-auto pt-3">
          <Link
            href={`/phone/${phone.id}`}
            className="inline-flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            View Details
          </Link>
        </div>
      </div>
    </article>
  );
}

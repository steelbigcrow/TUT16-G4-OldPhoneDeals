import React from 'react';
import { render, screen } from '@testing-library/react';
import PhoneCard from '@/components/catalog/PhoneCard';

describe('PhoneCard', () => {
  const basePhone = {
    id: 'p1',
    title: 'Vintage Phone',
    brand: 'Apple',
    price: 1299,
    stock: 5,
    imageUrl: 'https://cdn.test/phone.jpg',
    averageRating: 4.5,
    reviewCount: 3,
    sellerName: 'Alice'
  };

  it('shows phone info, price formatting and rating', () => {
    render(<PhoneCard phone={basePhone} />);

    expect(
      screen.getByRole('img', { name: basePhone.title })
    ).toHaveAttribute('src', basePhone.imageUrl);
    expect(screen.getByText(basePhone.brand)).toBeInTheDocument();
    expect(screen.getByText(basePhone.title)).toBeInTheDocument();
    expect(screen.getByText('$1,299')).toBeInTheDocument();
    expect(screen.getByText(/4\.5.*\(3\)/)).toBeInTheDocument();
    expect(screen.getByText('Seller: Alice')).toBeInTheDocument();

    const detailsLink = screen.getByRole('link', { name: /View Details/i });
    expect(detailsLink).toHaveAttribute('href', '/phone/p1');
    expect(screen.getByText('5 in stock')).toHaveClass('text-gray-600');
  });

  it('renders fallback image and stock labels', () => {
    const phoneOutOfStock = { ...basePhone, imageUrl: undefined, stock: 0 };
    render(<PhoneCard phone={phoneOutOfStock} />);

    expect(screen.getByText(/No image available/i)).toBeInTheDocument();
    expect(screen.getByText('Out of stock')).toHaveClass('text-red-600');
  });

  it('highlights low stock counts', () => {
    const lowStockPhone = { ...basePhone, stock: 2 };
    render(<PhoneCard phone={lowStockPhone} />);

    expect(screen.getByText('Only 2 left')).toHaveClass('text-orange-600');
  });
});

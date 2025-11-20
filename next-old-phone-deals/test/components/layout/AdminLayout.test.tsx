import React from 'react';
import { render, screen } from '@testing-library/react';
import AdminLayout from '@/components/layout/AdminLayout';

describe('AdminLayout', () => {
  it('renders navigation links and children', () => {
    render(
      <AdminLayout>
        <div>Admin content</div>
      </AdminLayout>
    );

    expect(screen.getByText('Admin Panel')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/admin');
    expect(screen.getByRole('link', { name: 'Users' })).toHaveAttribute('href', '/admin/users');
    expect(screen.getByRole('link', { name: 'Listings' })).toHaveAttribute('href', '/admin/listings');
    expect(screen.getByRole('link', { name: 'Reviews' })).toHaveAttribute('href', '/admin/reviews');
    expect(screen.getByRole('link', { name: 'Orders' })).toHaveAttribute('href', '/admin/orders');
    expect(screen.getByRole('link', { name: 'Logs' })).toHaveAttribute('href', '/admin/logs');
    expect(screen.getByText('Admin content')).toBeInTheDocument();
  });
});

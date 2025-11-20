import React from 'react';
import { render, screen } from '@testing-library/react';
import Footer from '@/components/common/Footer';

describe('Footer', () => {
  it('renders copyright with current year', () => {
    const year = new Date().getFullYear();

    render(<Footer />);

    expect(screen.getByText(`© ${year} Old Phone Deals`)).toBeInTheDocument();
  });

  it('shows placeholder text for links', () => {
    render(<Footer />);

    expect(
      screen.getByText(/Next\.js frontend skeleton\s*[—-]\s*TODO: add footer links/i)
    ).toBeInTheDocument();
  });
});

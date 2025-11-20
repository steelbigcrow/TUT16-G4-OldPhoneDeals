import React from 'react';
import { render, screen } from '@testing-library/react';
import UserLayout from '@/components/layout/UserLayout';

jest.mock('@/components/common/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('@/components/common/Footer', () => () => <div data-testid="footer">Footer</div>);

describe('UserLayout', () => {
  it('wraps children with header and footer', () => {
    render(
      <UserLayout>
        <div>Child content</div>
      </UserLayout>
    );

    expect(screen.getByTestId('header')).toBeInTheDocument();
    expect(screen.getByTestId('footer')).toBeInTheDocument();
    expect(screen.getByText('Child content')).toBeInTheDocument();
  });
});

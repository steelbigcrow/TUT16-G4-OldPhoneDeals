import type { Metadata } from 'next';
import './globals.css';
import React from 'react';
import UserLayout from '@/components/layout/UserLayout';
import { AuthProvider } from '@/components/auth/AuthProvider';

export const metadata: Metadata = {
  title: 'Old Phone Deals',
  description: 'Next.js frontend skeleton for Old Phone Deals'
};

type RootLayoutProps = {
  children: React.ReactNode;
};

export default function RootLayout(props: RootLayoutProps) {
  const { children } = props;

  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50 text-gray-900">
        <AuthProvider>
          <UserLayout>{children}</UserLayout>
        </AuthProvider>
      </body>
    </html>
  );
}
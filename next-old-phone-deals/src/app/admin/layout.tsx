import React from 'react';
import type { Metadata } from 'next';
import AdminLayout from '@/components/layout/AdminLayout';

export const metadata: Metadata = {
  title: 'Admin | Old Phone Deals',
  description: 'Admin dashboard for Old Phone Deals'
};

type AdminLayoutWrapperProps = {
  children: React.ReactNode;
};

export default function AdminLayoutWrapper(
  props: AdminLayoutWrapperProps
): JSX.Element {
  const { children } = props;

  return <AdminLayout>{children}</AdminLayout>;
}
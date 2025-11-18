import React from 'react';
import Header from '@/components/common/Header';
import Footer from '@/components/common/Footer';

type UserLayoutProps = {
  children: React.ReactNode;
};

const UserLayout: React.FC<UserLayoutProps> = (props) => {
  const { children } = props;

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <Header />
      <main className="flex-1">
        <div className="mx-auto max-w-6xl px-4 py-6">{children}</div>
      </main>
      <Footer />
    </div>
  );
};

export default UserLayout;
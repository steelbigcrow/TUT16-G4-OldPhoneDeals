import React from 'react';
import Link from 'next/link';

type AdminLayoutProps = {
  children: React.ReactNode;
};

const AdminLayout: React.FC<AdminLayoutProps> = (props) => {
  const { children } = props;

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-50">
      <aside className="w-64 border-r border-slate-800 bg-slate-900">
        <div className="px-4 py-3 text-lg font-semibold">Admin Panel</div>
        <nav className="px-4 pb-4 text-sm">
          <ul className="space-y-1">
            <li>
              <Link
                href="/admin"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Dashboard
              </Link>
            </li>
            <li>
              <Link
                href="/admin/users"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Users
              </Link>
            </li>
            <li>
              <Link
                href="/admin/listings"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Listings
              </Link>
            </li>
            <li>
              <Link
                href="/admin/reviews"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Reviews
              </Link>
            </li>
            <li>
              <Link
                href="/admin/orders"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Orders
              </Link>
            </li>
            <li>
              <Link
                href="/admin/logs"
                className="flex items-center rounded px-2 py-1 hover:bg-slate-800"
              >
                Logs
              </Link>
            </li>
          </ul>
        </nav>
      </aside>
      <div className="flex flex-1 flex-col">
        <header className="border-b border-slate-800 bg-slate-900/80 px-6 py-3 text-sm">
          <span className="font-medium">Old Phone Deals Admin</span>
          <span className="ml-2 text-slate-400">— TODO: add admin toolbar</span>
        </header>
        <main className="flex-1 bg-slate-950 px-6 py-6">{children}</main>
      </div>
    </div>
  );
};

export default AdminLayout;
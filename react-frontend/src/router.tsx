import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/common/ProtectedRoute'
import { AppLayout } from './components/layout/AppLayout'
import {
  AdminDashboardPage,
  AdminHomePage,
  AdminLayoutPage,
  AdminLoginPage,
  AdminUsersPage,
  CheckoutPage,
  HomePage,
  LoginPage,
  NotFoundPage,
  PhoneDetailPage,
  ProfileHomePage,
  ProfileLayoutPage,
  ProfileSettingsPage,
  RegisterPage,
  ResetPasswordPage,
  SearchPage,
  VerifyEmailPage,
  WishlistPage,
} from './pages'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <Navigate to='home' replace />,
      },
      {
        path: 'home',
        element: <HomePage />,
      },
      {
        path: 'search',
        element: <SearchPage />,
      },
      {
        path: 'phone/:id',
        element: <PhoneDetailPage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        path: 'register',
        element: <RegisterPage />,
      },
      {
        path: 'verify-email',
        element: <VerifyEmailPage />,
      },
      {
        path: 'reset-password',
        element: <ResetPasswordPage />,
      },
      {
        path: 'admin/login',
        element: <AdminLoginPage />,
      },
      {
        element: <ProtectedRoute mode='user' />,
        children: [
          {
            path: 'checkout',
            element: <CheckoutPage />,
          },
          {
            path: 'wishlist',
            element: <WishlistPage />,
          },
          {
            path: 'profile',
            element: <ProfileLayoutPage />,
            children: [
              {
                index: true,
                element: <ProfileHomePage />,
              },
              {
                path: 'settings',
                element: <ProfileSettingsPage />,
              },
              {
                path: '*',
                element: <NotFoundPage />,
              },
            ],
          },
        ],
      },
      {
        element: <ProtectedRoute mode='admin' />,
        children: [
          {
            path: 'admin',
            element: <AdminLayoutPage />,
            children: [
              {
                index: true,
                element: <AdminHomePage />,
              },
              {
                path: 'dashboard',
                element: <AdminDashboardPage />,
              },
              {
                path: 'users',
                element: <AdminUsersPage />,
              },
              {
                path: '*',
                element: <NotFoundPage />,
              },
            ],
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
])
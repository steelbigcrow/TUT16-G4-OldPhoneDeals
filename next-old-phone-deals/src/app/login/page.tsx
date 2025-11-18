'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { apiPost } from '@/lib/apiClient';
import { useAuth } from '@/components/auth/AuthProvider';
import type { ApiResponse, LoginResponse, LoginRequest } from '@/types/auth';

type LoginMode = 'user' | 'admin';

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { loginUser, loginAdmin, user, admin } = useAuth();

  const [mode, setMode] = useState<LoginMode>('user');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // 从 URL 参数读取模式
  useEffect(() => {
    const modeParam = searchParams.get('mode');
    if (modeParam === 'admin') {
      setMode('admin');
    }
  }, [searchParams]);

  // 如果已登录，重定向
  useEffect(() => {
    if (user) {
      router.push('/');
    } else if (admin) {
      router.push('/admin');
    }
  }, [user, admin, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const loginData: LoginRequest = { email, password };

      if (mode === 'user') {
        // 用户登录
        const response = await apiPost<ApiResponse<LoginResponse>>(
          '/auth/login',
          loginData
        );

        if (response.success && response.data) {
          const { token, user: userData } = response.data;
          if (token && userData) {
            loginUser(token, userData);
            const redirect = searchParams.get('redirect') || '/';
            router.push(redirect);
          }
        } else {
          setError(response.message || 'Login failed');
        }
      } else {
        // 管理员登录
        const response = await apiPost<ApiResponse<LoginResponse>>(
          '/admin/login',
          loginData
        );

        if (response.success && response.data) {
          const { token, admin: adminData } = response.data;
          if (token && adminData) {
            loginAdmin(token, adminData);
            router.push('/admin');
          }
        } else {
          setError(response.message || 'Admin login failed');
        }
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message || 'An error occurred during login');
      } else {
        setError('An error occurred during login');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-md px-4 py-8">
      <h1 className="mb-6 text-center text-2xl font-bold">Login</h1>

      {/* 模式切换 */}
      <div className="mb-6 flex rounded-lg border border-gray-300">
        <button
          type="button"
          onClick={() => setMode('user')}
          className={`flex-1 rounded-l-lg px-4 py-2 text-sm font-medium ${
            mode === 'user'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          User Login
        </button>
        <button
          type="button"
          onClick={() => setMode('admin')}
          className={`flex-1 rounded-r-lg px-4 py-2 text-sm font-medium ${
            mode === 'admin'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          Admin Login
        </button>
      </div>

      {/* 登录表单 */}
      <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border bg-white p-6 shadow">
        {error && (
          <div className="rounded bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-700">
            Email
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            placeholder="your@email.com"
          />
        </div>

        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-700">
            Password
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            placeholder="••••••••"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:bg-gray-400"
        >
          {isLoading ? 'Logging in...' : `Login as ${mode === 'user' ? 'User' : 'Admin'}`}
        </button>

        {mode === 'user' && (
          <div className="space-y-2 text-center text-sm">
            <div>
              <Link href="/register" className="text-blue-600 hover:underline">
                Don&apos;t have an account? Register
              </Link>
            </div>
            <div>
              <Link href="/reset-password" className="text-blue-600 hover:underline">
                Forgot password?
              </Link>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
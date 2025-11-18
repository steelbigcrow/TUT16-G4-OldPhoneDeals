'use client';

import React, { useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { apiPost } from '@/lib/apiClient';
import type { ApiResponse, VerifyEmailRequest } from '@/types/auth';

type VerifyStatus = 'loading' | 'success' | 'error';

export default function VerifyEmailPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [status, setStatus] = useState<VerifyStatus>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const verifyEmail = async () => {
      const token = searchParams.get('token');
      const email = searchParams.get('email');

      if (!token || !email) {
        setStatus('error');
        setMessage('Invalid verification link. Missing token or email.');
        return;
      }

      try {
        const verifyData: VerifyEmailRequest = {
          email,
          token
        };

        const response = await apiPost<ApiResponse<void>>(
          '/auth/verify-email',
          verifyData
        );

        if (response.success) {
          setStatus('success');
          setMessage(response.message || 'Email verified successfully!');
          // 3 秒后跳转到登录页
          setTimeout(() => {
            router.push('/login');
          }, 3000);
        } else {
          setStatus('error');
          setMessage(response.message || 'Email verification failed');
        }
      } catch (err: unknown) {
        setStatus('error');
        if (err instanceof Error) {
          setMessage(err.message || 'An error occurred during verification');
        } else {
          setMessage('An error occurred during verification');
        }
      }
    };

    verifyEmail();
  }, [searchParams, router]);

  return (
    <div className="mx-auto max-w-md px-4 py-8">
      <div className="rounded-lg border bg-white p-6 shadow">
        {status === 'loading' && (
          <div className="text-center">
            <div className="mx-auto mb-4 h-16 w-16 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
            <h2 className="mb-2 text-xl font-bold text-gray-900">
              Verifying Your Email...
            </h2>
            <p className="text-gray-600">Please wait while we verify your email address.</p>
          </div>
        )}

        {status === 'success' && (
          <div className="text-center">
            <div className="mb-4 text-green-600">
              <svg
                className="mx-auto h-16 w-16"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <h2 className="mb-4 text-xl font-bold text-gray-900">
              Email Verified!
            </h2>
            <p className="mb-4 text-gray-600">{message}</p>
            <p className="mb-4 text-sm text-gray-500">
              You can now log in to your account.
            </p>
            <p className="text-sm text-gray-500">
              Redirecting to login page...
            </p>
            <div className="mt-4">
              <Link
                href="/login"
                className="inline-block rounded bg-blue-600 px-6 py-2 text-white hover:bg-blue-700"
              >
                Go to Login
              </Link>
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="text-center">
            <div className="mb-4 text-red-600">
              <svg
                className="mx-auto h-16 w-16"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <h2 className="mb-4 text-xl font-bold text-gray-900">
              Verification Failed
            </h2>
            <p className="mb-4 text-gray-600">{message}</p>
            <p className="mb-4 text-sm text-gray-500">
              The verification link may be invalid or expired.
            </p>
            <div className="space-y-2">
              <div>
                <Link
                  href="/login"
                  className="inline-block rounded bg-blue-600 px-6 py-2 text-white hover:bg-blue-700"
                >
                  Go to Login
                </Link>
              </div>
              <div>
                <Link
                  href="/register"
                  className="text-sm text-blue-600 hover:underline"
                >
                  Register a new account
                </Link>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
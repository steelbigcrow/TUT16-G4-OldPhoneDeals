'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { changePassword } from '@/lib/profileApi';

export default function ChangePasswordPage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile/change-password');
    }
  }, [user, userToken, isLoading, router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !userToken) return;

    // 前端验证
    if (!formData.currentPassword) {
      setError('请输入当前密码');
      return;
    }

    if (formData.newPassword.length < 8) {
      setError('新密码长度至少为 8 位');
      return;
    }

    if (formData.newPassword !== formData.confirmNewPassword) {
      setError('两次输入的新密码不一致');
      return;
    }

    if (formData.currentPassword === formData.newPassword) {
      setError('新密码不能与当前密码相同');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      setSuccess(false);

      await changePassword(
        user.id,
        {
          currentPassword: formData.currentPassword,
          newPassword: formData.newPassword
        },
        userToken
      );

      setSuccess(true);
      // 清空表单
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: ''
      });

      // 3秒后跳转回用户中心
      setTimeout(() => {
        router.push('/user/profile');
      }, 3000);
    } catch (err: any) {
      console.error('修改密码失败:', err);
      setError(err.message || '修改密码失败，请检查当前密码是否正确');
    } finally {
      setSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <section className="max-w-md space-y-4">
        <div className="text-center py-8">加载中...</div>
      </section>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <section className="max-w-md space-y-6">
      <div>
        <h1 className="text-3xl font-bold">修改密码</h1>
        <p className="text-gray-600 mt-2">请输入当前密码和新密码</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
          <div className="font-semibold">密码修改成功！</div>
          <div className="text-sm mt-1">下次登录请使用新密码。即将返回个人中心...</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 mb-1">
            当前密码 *
          </label>
          <input
            type="password"
            id="currentPassword"
            name="currentPassword"
            value={formData.currentPassword}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={submitting || success}
          />
        </div>

        <div>
          <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">
            新密码 *
          </label>
          <input
            type="password"
            id="newPassword"
            name="newPassword"
            value={formData.newPassword}
            onChange={handleChange}
            required
            minLength={8}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={submitting || success}
          />
          <p className="text-xs text-gray-500 mt-1">密码长度至少 8 位</p>
        </div>

        <div>
          <label htmlFor="confirmNewPassword" className="block text-sm font-medium text-gray-700 mb-1">
            确认新密码 *
          </label>
          <input
            type="password"
            id="confirmNewPassword"
            name="confirmNewPassword"
            value={formData.confirmNewPassword}
            onChange={handleChange}
            required
            minLength={8}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={submitting || success}
          />
        </div>

        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            disabled={submitting || success}
            className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? '提交中...' : '修改密码'}
          </button>
          <button
            type="button"
            onClick={() => router.push('/user/profile')}
            disabled={submitting}
            className="flex-1 bg-gray-200 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:cursor-not-allowed transition-colors"
          >
            取消
          </button>
        </div>
      </form>
    </section>
  );
}
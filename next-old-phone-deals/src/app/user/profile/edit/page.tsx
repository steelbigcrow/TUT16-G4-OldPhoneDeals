'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getProfile, updateProfile } from '@/lib/profileApi';
import type { UpdateProfileRequest } from '@/types/profile';

export default function EditProfilePage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [formData, setFormData] = useState<UpdateProfileRequest>({
    firstName: '',
    lastName: ''
  });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile/edit');
      return;
    }

    const fetchProfile = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getProfile(user.id, userToken);
        setFormData({
          firstName: data.firstName,
          lastName: data.lastName
        });
      } catch (err) {
        console.error('获取用户资料失败:', err);
        setError('获取用户资料失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [user, userToken, isLoading, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !userToken) return;

    // 简单验证
    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setError('姓名不能为空');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      setSuccess(false);

      await updateProfile(user.id, formData, userToken);
      setSuccess(true);

      // 3秒后跳转回用户中心
      setTimeout(() => {
        router.push('/user/profile');
      }, 2000);
    } catch (err: any) {
      console.error('更新资料失败:', err);
      setError(err.message || '更新资料失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (isLoading || loading) {
    return (
      <section className="max-w-md space-y-4">
        <div className="text-center py-8">加载中...</div>
      </section>
    );
  }

  return (
    <section className="max-w-md space-y-6">
      <div>
        <h1 className="text-3xl font-bold">编辑个人资料</h1>
        <p className="text-gray-600 mt-2">更新您的个人信息</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
          保存成功！即将返回个人中心...
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
            名字 *
          </label>
          <input
            type="text"
            id="firstName"
            name="firstName"
            value={formData.firstName}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={submitting}
          />
        </div>

        <div>
          <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
            姓氏 *
          </label>
          <input
            type="text"
            id="lastName"
            name="lastName"
            value={formData.lastName}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={submitting}
          />
        </div>

        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? '保存中...' : '保存'}
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
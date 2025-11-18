'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getProfile } from '@/lib/profileApi';
import type { UserProfileResponse } from '@/types/profile';

export default function UserProfileHomePage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile');
      return;
    }

    const fetchProfile = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getProfile(user.id, userToken);
        setProfile(data);
      } catch (err) {
        console.error('获取用户资料失败:', err);
        setError('获取用户资料失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [user, userToken, isLoading, router]);

  if (isLoading || loading) {
    return (
      <section className="space-y-4">
        <div className="text-center py-8">加载中...</div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="space-y-4">
        <div className="text-center py-8 text-red-600">{error}</div>
      </section>
    );
  }

  if (!profile) {
    return (
      <section className="space-y-4">
        <div className="text-center py-8">未找到用户资料</div>
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <h1 className="text-3xl font-bold">个人中心</h1>

      {/* 用户基本信息 */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-xl font-semibold mb-4">基本信息</h2>
        <div className="space-y-2">
          <div className="flex items-center">
            <span className="text-gray-600 w-24">姓名：</span>
            <span className="font-medium">{profile.firstName} {profile.lastName}</span>
          </div>
          <div className="flex items-center">
            <span className="text-gray-600 w-24">邮箱：</span>
            <span className="font-medium">{profile.email}</span>
          </div>
          <div className="flex items-center">
            <span className="text-gray-600 w-24">邮箱验证：</span>
            <span className={profile.isVerified ? 'text-green-600' : 'text-yellow-600'}>
              {profile.isVerified ? '已验证' : '未验证'}
            </span>
          </div>
          <div className="flex items-center">
            <span className="text-gray-600 w-24">注册时间：</span>
            <span>{new Date(profile.createdAt).toLocaleDateString('zh-CN')}</span>
          </div>
          {profile.lastLoginAt && (
            <div className="flex items-center">
              <span className="text-gray-600 w-24">最后登录：</span>
              <span>{new Date(profile.lastLoginAt).toLocaleString('zh-CN')}</span>
            </div>
          )}
        </div>
      </div>

      {/* 快捷入口 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Link
          href="/user/profile/edit"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">编辑资料</h3>
          <p className="text-sm text-gray-600">修改个人信息</p>
        </Link>

        <Link
          href="/user/profile/change-password"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">修改密码</h3>
          <p className="text-sm text-gray-600">更新登录密码</p>
        </Link>

        <Link
          href="/user/profile/listings"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">我的挂牌</h3>
          <p className="text-sm text-gray-600">管理发布的手机</p>
        </Link>

        <Link
          href="/user/profile/reviews"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">卖家收到的评论</h3>
          <p className="text-sm text-gray-600">查看买家评价</p>
        </Link>

        <Link
          href="/user/profile/orders"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">我的订单</h3>
          <p className="text-sm text-gray-600">查看购买记录</p>
        </Link>

        <Link
          href="/wishlist"
          className="bg-white shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
        >
          <h3 className="text-lg font-semibold mb-2">我的心愿单</h3>
          <p className="text-sm text-gray-600">查看收藏的商品</p>
        </Link>
      </div>
    </section>
  );
}
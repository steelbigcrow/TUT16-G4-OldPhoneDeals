'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getAdminToken } from '@/lib/authStorage';
import { getDashboardStats } from '@/lib/adminDashboardApi';
import type { DashboardStatsResponse } from '@/types/admin';

export default function AdminDashboardPage(): JSX.Element {
  const { admin, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 检查管理员登录状态
  useEffect(() => {
    if (!authLoading && !admin) {
      router.push('/login?mode=admin&redirect=/admin');
    }
  }, [admin, authLoading, router]);

  // 加载仪表盘统计数据
  useEffect(() => {
    const loadStats = async () => {
      const token = getAdminToken();
      if (!token || !admin) return;

      try {
        setLoading(true);
        setError(null);
        const data = await getDashboardStats(token);
        setStats(data);
      } catch (err) {
        const message = err instanceof Error ? err.message : '加载仪表盘数据失败';
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    if (admin) {
      loadStats();
    }
  }, [admin]);

  // Loading 状态
  if (authLoading || !admin) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-slate-400">正在加载...</p>
      </div>
    );
  }

  // Error 状态
  if (error && !loading) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-semibold">管理员仪表盘</h1>
        <div className="rounded-lg bg-red-500/10 border border-red-500/20 p-4">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-2 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            重试
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">管理员仪表盘</h1>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-400">正在加载统计数据...</p>
        </div>
      ) : stats ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* 总用户数 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">总用户数</h3>
            <p className="text-3xl font-bold text-blue-400">{stats.totalUsers}</p>
          </div>

          {/* 总挂牌数 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">总挂牌数</h3>
            <p className="text-3xl font-bold text-green-400">{stats.totalListings}</p>
          </div>

          {/* 总评论数 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">总评论数</h3>
            <p className="text-3xl font-bold text-yellow-400">{stats.totalReviews}</p>
          </div>

          {/* 总订单数 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">总订单数</h3>
            <p className="text-3xl font-bold text-purple-400">{stats.totalOrders}</p>
          </div>

          {/* 总销售额 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">总销售额</h3>
            <p className="text-3xl font-bold text-emerald-400">
              ${stats.totalRevenue.toFixed(2)}
            </p>
          </div>

          {/* 平均订单金额 */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h3 className="text-sm text-slate-400 mb-2">平均订单金额</h3>
            <p className="text-3xl font-bold text-orange-400">
              ${stats.totalOrders > 0 ? (stats.totalRevenue / stats.totalOrders).toFixed(2) : '0.00'}
            </p>
          </div>
        </div>
      ) : null}

      {/* 最近销售（可选） */}
      {stats?.recentSales && stats.recentSales.length > 0 && (
        <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
          <h2 className="text-lg font-semibold mb-4">最近销售</h2>
          <div className="space-y-2">
            {stats.recentSales.map((sale, index) => (
              <div
                key={index}
                className="flex justify-between items-center py-2 border-b border-slate-700 last:border-b-0"
              >
                <span className="text-slate-400">{sale.date}</span>
                <div className="text-right">
                  <span className="text-sm text-slate-400 mr-4">{sale.count} 笔订单</span>
                  <span className="font-semibold text-emerald-400">
                    ${sale.revenue.toFixed(2)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
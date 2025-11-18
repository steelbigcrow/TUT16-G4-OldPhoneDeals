'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/components/auth/AuthProvider';
import {
  getSellerListings,
  deletePhone,
  togglePhoneDisabled
} from '@/lib/sellerListingsApi';
import type { PhoneResponse } from '@/types/phone';

export default function UserListingsPage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [listings, setListings] = useState<PhoneResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterBrand, setFilterBrand] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'enabled' | 'disabled'>('all');

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile/listings');
      return;
    }

    fetchListings();
  }, [user, userToken, isLoading, router]);

  const fetchListings = async () => {
    if (!user || !userToken) return;

    try {
      setLoading(true);
      setError('');
      const data = await getSellerListings(user.id, userToken);
      setListings(data);
    } catch (err) {
      console.error('获取挂牌列表失败:', err);
      setError('获取挂牌列表失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (phoneId: string, title: string) => {
    if (!userToken) return;
    if (!confirm(`确定要删除商品"${title}"吗？此操作无法撤销。`)) return;

    try {
      await deletePhone(phoneId, userToken);
      // 刷新列表
      await fetchListings();
    } catch (err: any) {
      console.error('删除商品失败:', err);
      alert(err.message || '删除商品失败，请稍后重试');
    }
  };

  const handleToggleStatus = async (phoneId: string, currentStatus: boolean) => {
    if (!userToken) return;

    try {
      await togglePhoneDisabled(phoneId, !currentStatus, userToken);
      // 刷新列表
      await fetchListings();
    } catch (err: any) {
      console.error('切换商品状态失败:', err);
      alert(err.message || '操作失败，请稍后重试');
    }
  };

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

  // 过滤列表
  const filteredListings = listings.filter(phone => {
    if (filterBrand && phone.brand !== filterBrand) return false;
    if (filterStatus === 'enabled' && phone.isDisabled) return false;
    if (filterStatus === 'disabled' && !phone.isDisabled) return false;
    return true;
  });

  // 获取所有品牌用于筛选
  const brands = Array.from(new Set(listings.map(p => p.brand)));

  return (
    <section className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">我的挂牌手机</h1>
        <Link
          href="/phones/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
        >
          + 新增挂牌
        </Link>
      </div>

      {/* 筛选栏 */}
      <div className="bg-white shadow rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              品牌筛选
            </label>
            <select
              value={filterBrand}
              onChange={(e) => setFilterBrand(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">全部品牌</option>
              {brands.map(brand => (
                <option key={brand} value={brand}>{brand}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              状态筛选
            </label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as any)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">全部状态</option>
              <option value="enabled">已上架</option>
              <option value="disabled">已下架</option>
            </select>
          </div>
        </div>
      </div>

      {/* 列表 */}
      {filteredListings.length === 0 ? (
        <div className="bg-white shadow rounded-lg p-8 text-center text-gray-500">
          {listings.length === 0 ? '暂无挂牌商品' : '没有符合条件的商品'}
        </div>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  商品信息
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  价格
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  库存
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  销量
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  状态
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredListings.map((phone) => (
                <tr key={phone.id}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      {phone.imageUrl && (
                        <img
                          src={phone.imageUrl}
                          alt={phone.title}
                          className="h-10 w-10 rounded object-cover mr-3"
                        />
                      )}
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {phone.title}
                        </div>
                        <div className="text-sm text-gray-500">{phone.brand}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${phone.price}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {phone.stock}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {phone.salesCount}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        phone.isDisabled
                          ? 'bg-red-100 text-red-800'
                          : 'bg-green-100 text-green-800'
                      }`}
                    >
                      {phone.isDisabled ? '已下架' : '已上架'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                    <Link
                      href={`/phones/${phone.id}`}
                      className="text-blue-600 hover:text-blue-900"
                    >
                      查看
                    </Link>
                    <button
                      onClick={() => handleToggleStatus(phone.id, phone.isDisabled)}
                      className="text-yellow-600 hover:text-yellow-900"
                    >
                      {phone.isDisabled ? '上架' : '下架'}
                    </button>
                    <button
                      onClick={() => handleDelete(phone.id, phone.title)}
                      className="text-red-600 hover:text-red-900"
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="text-sm text-gray-500">
        共 {filteredListings.length} 件商品
        {filteredListings.length !== listings.length && ` (从 ${listings.length} 件筛选)`}
      </div>
    </section>
  );
}
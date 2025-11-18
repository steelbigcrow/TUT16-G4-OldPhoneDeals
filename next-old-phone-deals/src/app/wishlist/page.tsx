'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getUserToken } from '@/lib/authStorage';
import { getWishlist, removeFromWishlist } from '@/lib/wishlistApi';
import { buildMediaUrl } from '@/lib/mediaUrl';
import type { WishlistResponse, WishlistPhone } from '@/types/wishlist';

export default function WishlistPage(): JSX.Element {
  const router = useRouter();
  const { user } = useAuth();

  const [wishlist, setWishlist] = useState<WishlistResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removing, setRemoving] = useState<string | null>(null);

  // 检查登录状态
  useEffect(() => {
    if (!user) {
      router.push('/login?redirect=/wishlist');
    }
  }, [user, router]);

  // 加载心愿单
  useEffect(() => {
    const fetchWishlist = async () => {
      const token = getUserToken();
      if (!token) {
        router.push('/login?redirect=/wishlist');
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const data = await getWishlist(token);
        setWishlist({
          ...data,
          phones:
            data.phones?.map((phone) => ({
              ...phone,
              image: buildMediaUrl(phone.image)
            })) ?? []
        });
      } catch (err) {
        console.error('Failed to fetch wishlist:', err);
        setError(err instanceof Error ? err.message : '加载心愿单失败');
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchWishlist();
    }
  }, [user, router]);

  // 移除商品
  const handleRemove = async (phoneId: string) => {
    const token = getUserToken();
    if (!token) return;

    if (!confirm('确定要从心愿单中移除该商品吗？')) {
      return;
    }

    try {
      setRemoving(phoneId);
      const updatedWishlist = await removeFromWishlist(token, phoneId);
      setWishlist(updatedWishlist);
    } catch (err) {
      console.error('Failed to remove from wishlist:', err);
      alert(err instanceof Error ? err.message : '移除失败');
    } finally {
      setRemoving(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-gray-600">正在加载心愿单...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-semibold text-red-600">加载失败</h1>
        <p className="text-gray-700">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          重试
        </button>
      </div>
    );
  }

  if (!wishlist || wishlist.phones.length === 0) {
    return (
      <div className="text-center py-12 space-y-4">
        <h1 className="text-2xl font-semibold">心愿单为空</h1>
        <p className="text-gray-600">还没有收藏任何商品</p>
        <button
          onClick={() => router.push('/')}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          去逛逛
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">我的心愿单</h1>
        <p className="text-gray-600">共 {wishlist.items.length} 件商品</p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {wishlist.phones.map((phone: WishlistPhone) => {
          return (
            <div
              key={phone.id}
              className="border rounded-lg overflow-hidden hover:shadow-lg transition"
            >
              {/* 商品图片 */}
              <div className="relative h-48 bg-gray-200">
                {phone.image ? (
                  <img
                    src={phone.image}
                    alt={phone.title}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <span className="text-gray-500">暂无图片</span>
                  </div>
                )}
                {phone.isDisabled && (
                  <div className="absolute top-2 right-2 bg-red-600 text-white text-xs px-2 py-1 rounded">
                    已下架
                  </div>
                )}
              </div>

              {/* 商品信息 */}
              <div className="p-4 space-y-3">
                <h3 className="font-semibold text-lg line-clamp-2">
                  {phone.title}
                </h3>
                <p className="text-sm text-gray-600">{phone.brand}</p>
                <p className="text-2xl font-bold text-blue-600">
                  ${phone.price.toFixed(2)}
                </p>
                <p className="text-sm text-gray-500">
                  库存：{phone.stock > 0 ? `${phone.stock} 件` : '缺货'}
                </p>

                {phone.seller && (
                  <p className="text-sm text-gray-600">
                    卖家：
                    {[phone.seller.firstName, phone.seller.lastName]
                      .filter(Boolean)
                      .join(' ') || '认证卖家'}
                  </p>
                )}

                {/* 操作按钮 */}
                <div className="flex gap-2 pt-2">
                  <button
                    onClick={() => router.push(`/phone/${phone.id}`)}
                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
                  >
                    查看详情
                  </button>
                  <button
                    onClick={() => handleRemove(phone.id)}
                    disabled={removing === phone.id}
                    className="px-4 py-2 border border-red-600 text-red-600 rounded hover:bg-red-50 transition disabled:opacity-50"
                  >
                    {removing === phone.id ? '移除中...' : '移除'}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

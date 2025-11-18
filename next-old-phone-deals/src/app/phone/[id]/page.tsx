'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getUserToken } from '@/lib/authStorage';
import { apiGet } from '@/lib/apiClient';
import { buildMediaUrl } from '@/lib/mediaUrl';
import { addToCart } from '@/lib/cartApi';
import { addToWishlist } from '@/lib/wishlistApi';
import type { ApiResponse } from '@/types/auth';

type PhoneDetailPageProps = {
  params: {
    id: string;
  };
};

type ApiSeller = {
  id?: string;
  firstName?: string;
  lastName?: string;
};

type ApiPhone = {
  id: string;
  title: string;
  brand: string;
  price: number;
  stock: number;
  description?: string;
  image?: string;
  isDisabled?: boolean;
  seller?: ApiSeller;
};

interface Phone {
  id: string;
  title: string;
  brand: string;
  price: number;
  stock: number;
  description?: string;
  imageUrl?: string;
  isDisabled?: boolean;
  seller?: ApiSeller;
}

function normalizePhoneData(apiPhone: ApiPhone): Phone {
  return {
    id: apiPhone.id,
    title: apiPhone.title,
    brand: apiPhone.brand,
    price: apiPhone.price,
    stock: apiPhone.stock,
    description: apiPhone.description,
    imageUrl: buildMediaUrl(apiPhone.image),
    isDisabled: apiPhone.isDisabled ?? false,
    seller: apiPhone.seller
  };
}

export default function PhoneDetailPage(
  props: PhoneDetailPageProps
): JSX.Element {
  const { params } = props;
  const router = useRouter();
  const { user } = useAuth();

  const [phone, setPhone] = useState<Phone | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addingToCart, setAddingToCart] = useState(false);
  const [addingToWishlist, setAddingToWishlist] = useState(false);
  const [message, setMessage] = useState<{
    type: 'success' | 'error';
    text: string;
  } | null>(null);

  // 加载手机详情
  useEffect(() => {
    const fetchPhone = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await apiGet<ApiResponse<ApiPhone>>(
          `/phones/${params.id}`
        );

        if (response.success && response.data) {
          setPhone(normalizePhoneData(response.data));
        } else {
          setError(response.message || '加载失败');
        }
      } catch (err) {
        console.error('Failed to fetch phone:', err);
        setError(err instanceof Error ? err.message : '加载手机详情失败');
      } finally {
        setLoading(false);
      }
    };

    fetchPhone();
  }, [params.id]);

  // 加入购物车
  const handleAddToCart = async () => {
    if (!user) {
      setMessage({
        type: 'error',
        text: '请先登录'
      });
      setTimeout(() => {
        router.push(`/login?redirect=/phone/${params.id}`);
      }, 1500);
      return;
    }

    const token = getUserToken();
    if (!token) {
      setMessage({
        type: 'error',
        text: '请先登录'
      });
      return;
    }

    try {
      setAddingToCart(true);
      setMessage(null);
      await addToCart(token, {
        phoneId: params.id,
        quantity: 1
      });
      setMessage({
        type: 'success',
        text: '已加入购物车'
      });
    } catch (err) {
      console.error('Failed to add to cart:', err);
      setMessage({
        type: 'error',
        text: err instanceof Error ? err.message : '加入购物车失败'
      });
    } finally {
      setAddingToCart(false);
    }
  };

  // 加入心愿单
  const handleAddToWishlist = async () => {
    if (!user) {
      setMessage({
        type: 'error',
        text: '请先登录'
      });
      setTimeout(() => {
        router.push(`/login?redirect=/phone/${params.id}`);
      }, 1500);
      return;
    }

    const token = getUserToken();
    if (!token) {
      setMessage({
        type: 'error',
        text: '请先登录'
      });
      return;
    }

    try {
      setAddingToWishlist(true);
      setMessage(null);
      await addToWishlist(token, params.id);
      setMessage({
        type: 'success',
        text: '已加入心愿单'
      });
    } catch (err) {
      console.error('Failed to add to wishlist:', err);
      setMessage({
        type: 'error',
        text: err instanceof Error ? err.message : '加入心愿单失败'
      });
    } finally {
      setAddingToWishlist(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-gray-600">加载中...</p>
      </div>
    );
  }

  if (error || !phone) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-semibold text-red-600">加载失败</h1>
        <p className="text-gray-700">{error || '未找到该商品'}</p>
        <button
          onClick={() => router.push('/')}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          返回首页
        </button>
      </div>
    );
  }

  const sellerName =
    phone.seller && (phone.seller.firstName || phone.seller.lastName)
      ? [phone.seller.firstName, phone.seller.lastName]
          .filter(Boolean)
          .join(' ')
          .trim()
      : '';

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* 消息提示 */}
      {message && (
        <div
          className={`p-4 rounded ${
            message.type === 'success'
              ? 'bg-green-100 text-green-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          {message.text}
          {message.type === 'success' && (
            <button
              onClick={() => router.push('/checkout')}
              className="ml-4 underline hover:no-underline"
            >
              去结算
            </button>
          )}
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-8">
        {/* 左侧：图片 */}
        <div className="space-y-4">
          {phone.imageUrl ? (
            <img
              src={phone.imageUrl}
              alt={phone.title}
              className="w-full h-auto rounded-lg shadow-md"
            />
          ) : (
            <div className="w-full h-96 bg-gray-200 rounded-lg flex items-center justify-center">
              <span className="text-gray-500">暂无图片</span>
            </div>
          )}
        </div>

        {/* 右侧：商品信息与操作 */}
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{phone.title}</h1>
            <p className="text-sm text-gray-500 mt-2">品牌：{phone.brand}</p>
          </div>

          <div className="space-y-2">
            <p className="text-3xl font-semibold text-blue-600">
              ${phone.price.toFixed(2)}
            </p>
            <p className="text-sm text-gray-600">
              库存：{phone.stock > 0 ? `${phone.stock} 件` : '缺货'}
            </p>
            {phone.isDisabled && (
              <p className="text-sm text-red-600">该商品已下架</p>
            )}
          </div>

          {phone.description && (
            <div className="space-y-2">
              <h2 className="text-lg font-semibold">商品描述</h2>
              <p className="text-gray-700 whitespace-pre-wrap">
                {phone.description}
              </p>
            </div>
          )}

          {phone.seller && (
            <div className="space-y-2">
              <h2 className="text-lg font-semibold">卖家信息</h2>
              <p className="text-gray-700">
                卖家：{sellerName || '认证卖家'}
              </p>
            </div>
          )}

          {/* 操作按钮 */}
          <div className="flex gap-4">
            <button
              onClick={handleAddToCart}
              disabled={
                addingToCart || phone.stock <= 0 || phone.isDisabled || false
              }
              className={`flex-1 px-6 py-3 rounded-lg font-semibold transition ${
                addingToCart || phone.stock <= 0 || phone.isDisabled
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}
            >
              {addingToCart ? '添加中...' : '加入购物车'}
            </button>

            <button
              onClick={handleAddToWishlist}
              disabled={addingToWishlist || phone.isDisabled || false}
              className={`flex-1 px-6 py-3 rounded-lg font-semibold transition ${
                addingToWishlist || phone.isDisabled
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-green-600 text-white hover:bg-green-700'
              }`}
            >
              {addingToWishlist ? '添加中...' : '加入心愿单'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

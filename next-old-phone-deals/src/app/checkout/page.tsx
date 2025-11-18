'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getUserToken } from '@/lib/authStorage';
import {
  getCart,
  updateCartItem,
  removeCartItem
} from '@/lib/cartApi';
import { createOrder } from '@/lib/orderApi';
import { buildMediaUrl } from '@/lib/mediaUrl';
import type { CartResponse, CartItem } from '@/types/cart';
import type { ShippingAddress, CheckoutRequest } from '@/types/order';

function enhanceCartResponse(data: CartResponse): CartResponse {
  return {
    ...data,
    items:
      data.items?.map((item) => ({
        ...item,
        phone: item.phone
          ? {
              ...item.phone,
              image: buildMediaUrl(item.phone.image)
            }
          : undefined
      })) ?? []
  };
}

export default function CheckoutPage(): JSX.Element {
  const router = useRouter();
  const { user } = useAuth();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updating, setUpdating] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);
  const [orderId, setOrderId] = useState<string | null>(null);

  // 收货地址表单
  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    fullName: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'Australia',
    phone: ''
  });

  // 检查登录状态
  useEffect(() => {
    if (!user) {
      router.push('/login?redirect=/checkout');
    }
  }, [user, router]);

  // 加载购物车
  useEffect(() => {
    const fetchCart = async () => {
      const token = getUserToken();
      if (!token) {
        router.push('/login?redirect=/checkout');
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const data = await getCart(token);
        setCart(enhanceCartResponse(data));
      } catch (err) {
        console.error('Failed to fetch cart:', err);
        setError(err instanceof Error ? err.message : '加载购物车失败');
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchCart();
    }
  }, [user, router]);

  // 增加数量
  const handleIncreaseQuantity = async (item: CartItem) => {
    const token = getUserToken();
    if (!token) return;

    const newQuantity = item.quantity + 1;
    if (
      typeof item.phone?.stock === 'number' &&
      newQuantity > item.phone.stock
    ) {
      alert('库存不足');
      return;
    }

    try {
      setUpdating(item.phoneId);
      const updatedCart = await updateCartItem(
        token,
        item.phoneId,
        newQuantity
      );
      setCart(enhanceCartResponse(updatedCart));
    } catch (err) {
      console.error('Failed to update cart:', err);
      alert(err instanceof Error ? err.message : '更新失败');
    } finally {
      setUpdating(null);
    }
  };

  // 减少数量
  const handleDecreaseQuantity = async (item: CartItem) => {
    const token = getUserToken();
    if (!token) return;

    const newQuantity = item.quantity - 1;
    if (newQuantity < 1) {
      handleRemoveItem(item.phoneId);
      return;
    }

    try {
      setUpdating(item.phoneId);
      const updatedCart = await updateCartItem(token, item.phoneId, newQuantity);
      setCart(enhanceCartResponse(updatedCart));
    } catch (err) {
      console.error('Failed to update cart:', err);
      alert(err instanceof Error ? err.message : '更新失败');
    } finally {
      setUpdating(null);
    }
  };

  // 删除商品
  const handleRemoveItem = async (phoneId: string) => {
    const token = getUserToken();
    if (!token) return;

    if (!confirm('确定要删除该商品吗？')) {
      return;
    }

    try {
      setUpdating(phoneId);
      const updatedCart = await removeCartItem(token, phoneId);
      setCart(enhanceCartResponse(updatedCart));
    } catch (err) {
      console.error('Failed to remove item:', err);
      alert(err instanceof Error ? err.message : '删除失败');
    } finally {
      setUpdating(null);
    }
  };

  // 计算总价
  const calculateTotal = (): number => {
    if (!cart || !cart.items) return 0;
    return cart.items.reduce((sum, item) => {
      const price =
        (typeof item.phone?.price === 'number' ? item.phone.price : undefined) ??
        item.price ??
        0;
      return sum + price * item.quantity;
    }, 0);
  };

  // 提交订单
  const handleSubmitOrder = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!cart || cart.items.length === 0) {
      alert('购物车为空');
      return;
    }

    // 验证收货地址
    if (
      !shippingAddress.fullName ||
      !shippingAddress.addressLine1 ||
      !shippingAddress.city ||
      !shippingAddress.postalCode ||
      !shippingAddress.country
    ) {
      alert('请填写完整的收货信息');
      return;
    }

    const token = getUserToken();
    if (!token) {
      router.push('/login?redirect=/checkout');
      return;
    }

    try {
      setSubmitting(true);
      const addressPayload: CheckoutRequest['address'] = {
        street: [shippingAddress.addressLine1, shippingAddress.addressLine2]
          .filter(Boolean)
          .join(' ')
          .trim(),
        city: shippingAddress.city,
        state: shippingAddress.state || 'N/A',
        zip: shippingAddress.postalCode,
        country: shippingAddress.country
      };

      const order = await createOrder(token, { address: addressPayload });
      setOrderSuccess(true);
      setOrderId(order.id);
      // 清空购物车状态
      setCart((prev) => (prev ? { ...prev, items: [] } : prev));
    } catch (err) {
      console.error('Failed to create order:', err);
      alert(err instanceof Error ? err.message : '下单失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-gray-600">正在加载购物车...</p>
      </div>
    );
  }

  if (orderSuccess) {
    return (
      <div className="max-w-2xl mx-auto space-y-6 text-center py-12">
        <div className="text-6xl">✅</div>
        <h1 className="text-3xl font-bold text-green-600">下单成功！</h1>
        <p className="text-gray-700">
          订单编号：<span className="font-mono">{orderId}</span>
        </p>
        <div className="flex gap-4 justify-center">
          <button
            onClick={() => router.push('/')}
            className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            返回首页
          </button>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
          >
            继续购物
          </button>
        </div>
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

  if (!cart || cart.items.length === 0) {
    return (
      <div className="text-center py-12 space-y-4">
        <h1 className="text-2xl font-semibold">购物车为空</h1>
        <p className="text-gray-600">还没有添加任何商品</p>
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
    <div className="max-w-6xl mx-auto space-y-8">
      <h1 className="text-3xl font-bold">购物车与结算</h1>

      <div className="grid md:grid-cols-3 gap-8">
        {/* 左侧：购物车商品列表 */}
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-xl font-semibold">商品列表</h2>
          {cart.items.map((item) => {
            const phone = item.phone || {
              id: item.phoneId,
              title: item.title,
              brand: '商品',
              price: item.price ?? 0,
              stock: 0,
              image: undefined
            };

            const displayPrice =
              typeof phone.price === 'number' ? phone.price : item.price ?? 0;

            return (
              <div
                key={item.phoneId}
                className="flex gap-4 p-4 border rounded-lg"
              >
                {/* 商品图片 */}
                <div className="w-24 h-24 flex-shrink-0">
                  {phone.image ? (
                    <img
                      src={phone.image}
                      alt={phone.title}
                      className="w-full h-full object-cover rounded"
                    />
                  ) : (
                    <div className="w-full h-full bg-gray-200 rounded flex items-center justify-center">
                      <span className="text-xs text-gray-500">无图</span>
                    </div>
                  )}
                </div>

                {/* 商品信息 */}
                <div className="flex-1 space-y-2">
                  <h3 className="font-semibold">{phone.title}</h3>
                  <p className="text-sm text-gray-600">{phone.brand}</p>
                  <p className="text-lg font-bold text-blue-600">
                    ${displayPrice.toFixed(2)}
                  </p>
                  <p className="text-sm text-gray-500">
                    库存：
                    {phone.stock && phone.stock > 0
                      ? `${phone.stock} 件`
                      : '缺货'}
                  </p>
                </div>

                {/* 数量调整 */}
                <div className="flex flex-col items-end justify-between">
                  <button
                    onClick={() => handleRemoveItem(item.phoneId)}
                    disabled={updating === item.phoneId}
                    className="text-red-600 hover:text-red-800 text-sm"
                  >
                    删除
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleDecreaseQuantity(item)}
                      disabled={updating === item.phoneId}
                      className="w-8 h-8 border rounded hover:bg-gray-100 disabled:opacity-50"
                    >
                      −
                    </button>
                    <span className="w-12 text-center font-semibold">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => handleIncreaseQuantity(item)}
                      disabled={updating === item.phoneId}
                      className="w-8 h-8 border rounded hover:bg-gray-100 disabled:opacity-50"
                    >
                      +
                    </button>
                  </div>

                  <p className="text-sm font-semibold">
                    小计：${(displayPrice * item.quantity).toFixed(2)}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* 右侧：订单摘要与收货信息 */}
        <div className="space-y-6">
          {/* 订单摘要 */}
          <div className="p-6 border rounded-lg space-y-4">
            <h2 className="text-xl font-semibold">订单摘要</h2>
            <div className="space-y-2">
              <div className="flex justify-between text-gray-700">
                <span>商品总数</span>
                <span>{cart.items.length} 种</span>
              </div>
              <div className="flex justify-between text-gray-700">
                <span>总数量</span>
                <span>
                  {cart.items.reduce((sum, item) => sum + item.quantity, 0)} 件
                </span>
              </div>
              <div className="border-t pt-2 flex justify-between text-lg font-bold">
                <span>总价</span>
                <span className="text-blue-600">
                  ${calculateTotal().toFixed(2)}
                </span>
              </div>
            </div>
          </div>

          {/* 收货信息表单 */}
          <form onSubmit={handleSubmitOrder} className="p-6 border rounded-lg space-y-4">
            <h2 className="text-xl font-semibold">收货信息</h2>

            <div>
              <label className="block text-sm font-medium mb-1">
                收件人姓名 *
              </label>
              <input
                type="text"
                value={shippingAddress.fullName}
                onChange={(e) =>
                  setShippingAddress({
                    ...shippingAddress,
                    fullName: e.target.value
                  })
                }
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                地址 *
              </label>
              <input
                type="text"
                value={shippingAddress.addressLine1}
                onChange={(e) =>
                  setShippingAddress({
                    ...shippingAddress,
                    addressLine1: e.target.value
                  })
                }
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                地址补充
              </label>
              <input
                type="text"
                value={shippingAddress.addressLine2}
                onChange={(e) =>
                  setShippingAddress({
                    ...shippingAddress,
                    addressLine2: e.target.value
                  })
                }
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  城市 *
                </label>
                <input
                  type="text"
                  value={shippingAddress.city}
                  onChange={(e) =>
                    setShippingAddress({
                      ...shippingAddress,
                      city: e.target.value
                    })
                  }
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  州/省
                </label>
                <input
                  type="text"
                  value={shippingAddress.state}
                  onChange={(e) =>
                    setShippingAddress({
                      ...shippingAddress,
                      state: e.target.value
                    })
                  }
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  邮编 *
                </label>
                <input
                  type="text"
                  value={shippingAddress.postalCode}
                  onChange={(e) =>
                    setShippingAddress({
                      ...shippingAddress,
                      postalCode: e.target.value
                    })
                  }
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  国家 *
                </label>
                <input
                  type="text"
                  value={shippingAddress.country}
                  onChange={(e) =>
                    setShippingAddress({
                      ...shippingAddress,
                      country: e.target.value
                    })
                  }
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                联系电话
              </label>
              <input
                type="tel"
                value={shippingAddress.phone}
                onChange={(e) =>
                  setShippingAddress({
                    ...shippingAddress,
                    phone: e.target.value
                  })
                }
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <button
              type="submit"
              disabled={submitting || cart.items.length === 0}
              className={`w-full py-3 rounded-lg font-semibold transition ${
                submitting || cart.items.length === 0
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}
            >
              {submitting ? '提交中...' : '提交订单'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

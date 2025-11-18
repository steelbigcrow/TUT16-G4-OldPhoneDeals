'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/components/auth/AuthProvider';
import { getUserOrders } from '@/lib/orderApi';
import type { OrderResponse } from '@/types/order';

export default function UserOrdersPage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile/orders');
      return;
    }

    fetchOrders();
  }, [user, userToken, isLoading, router]);

  const fetchOrders = async () => {
    if (!user || !userToken) return;

    try {
      setLoading(true);
      setError('');
      const data = await getUserOrders(userToken, user.id);
      setOrders(data);
    } catch (err) {
      console.error('获取订单列表失败:', err);
      setError('获取订单列表失败，请稍后重试');
    } finally {
      setLoading(false);
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

  return (
    <section className="space-y-6">
      <h1 className="text-3xl font-bold">我的订单</h1>

      {orders.length === 0 ? (
        <div className="bg-white shadow rounded-lg p-8 text-center">
          <p className="text-gray-500 mb-4">您还没有任何订单</p>
          <Link
            href="/"
            className="inline-block bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            去购物
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <div key={order.id} className="bg-white shadow rounded-lg overflow-hidden">
              {/* 订单头部 */}
              <div className="bg-gray-50 px-6 py-3 border-b border-gray-200">
                <div className="flex justify-between items-center">
                  <div className="flex items-center gap-4 text-sm">
                    <span className="font-medium">订单号: {order.id}</span>
                    <span className="text-gray-500">
                      下单时间:{' '}
                      {order.createdAt
                        ? new Date(order.createdAt).toLocaleString('zh-CN')
                        : '-'}
                    </span>
                  </div>
                </div>
              </div>

              {/* 订单商品列表 */}
              <div className="px-6 py-4">
                <div className="space-y-3">
                  {order.items.map((item, index) => (
                    <div key={index} className="flex items-center gap-4">
                      <div className="flex-1">
                        <div className="font-medium">{item.title}</div>
                        <div className="text-sm text-gray-500">
                          ID: {item.phoneId}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-medium">${item.price.toFixed(2)}</div>
                        <div className="text-sm text-gray-500">x{item.quantity}</div>
                      </div>
                      <div className="font-medium text-right w-24">
                        ${(item.price * item.quantity).toFixed(2)}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 订单底部 - 收货地址和总价 */}
              <div className="bg-gray-50 px-6 py-4 border-t border-gray-200">
                <div className="flex justify-between items-start">
                  <div className="text-sm">
                    <div className="font-medium mb-1">收货地址</div>
                    {order.address ? (
                      <>
                        <div className="text-gray-600">{order.address.street}</div>
                        <div className="text-gray-600">
                          {order.address.city}, {order.address.state} {order.address.zip}
                        </div>
                        <div className="text-gray-600">{order.address.country}</div>
                      </>
                    ) : (
                      <div className="text-gray-600">暂无地址信息</div>
                    )}
                  </div>
                  <div className="text-right">
                    <div className="text-sm text-gray-600 mb-1">订单总额</div>
                    <div className="text-2xl font-bold text-blue-600">
                      ${order.totalAmount.toFixed(2)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {orders.length > 0 && (
        <div className="text-sm text-gray-500">共 {orders.length} 个订单</div>
      )}
    </section>
  );
}

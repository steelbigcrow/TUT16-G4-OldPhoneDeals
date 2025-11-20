'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getAdminToken } from '@/lib/authStorage';
import {
  getAdminOrders,
  getOrderDetail
} from '@/lib/adminOrderApi';
import type {
  OrderManagementResponse,
  OrderDetailResponse
} from '@/types/admin';

export default function AdminOrdersPage(): JSX.Element {
  const { admin, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [orders, setOrders] = useState<OrderManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<OrderDetailResponse | null>(null);
  const [showDetail, setShowDetail] = useState(false);
  const pageSize = 10;

  // 检查管理员登录状态
  useEffect(() => {
    if (!authLoading && !admin) {
      router.push('/login?mode=admin&redirect=/admin/orders');
    }
  }, [admin, authLoading, router]);

  // 加载订单列表
  const loadOrders = async (page: number = 1) => {
    const token = getAdminToken();
    if (!token || !admin) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getAdminOrders(token, { page, pageSize });
      setOrders(response.content);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotal(response.totalItems);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载订单列表失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (admin) {
      loadOrders(1);
    }
  }, [admin]);

  // 查看订单详情
  const handleViewDetail = async (orderId: string) => {
    const token = getAdminToken();
    if (!token) return;

    try {
      const detail = await getOrderDetail(token, orderId);
      setSelectedOrder(detail);
      setShowDetail(true);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载详情失败';
      alert(message);
    }
  };

  // 分页控制
  const handlePrevPage = () => {
    if (currentPage > 1) {
      loadOrders(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      loadOrders(currentPage + 1);
    }
  };

  // Loading 状态
  if (authLoading || !admin) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-slate-400">正在加载...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-semibold">订单管理</h1>
        <div className="text-sm text-slate-400">
          共 {total} 个订单
        </div>
      </div>

      {/* Error 状态 */}
      {error && (
        <div className="rounded-lg bg-red-500/10 border border-red-500/20 p-4">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => loadOrders(currentPage)}
            className="mt-2 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            重试
          </button>
        </div>
      )}

      {/* 订单列表 */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-400">正在加载订单列表...</p>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-900 border-b border-slate-700">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">订单号</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">用户</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">订单时间</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">商品数量</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">订单金额</th>
                  <th className="px-4 py-3 text-right text-sm font-semibold text-slate-300">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-700/50">
                    <td className="px-4 py-3 text-sm font-mono text-slate-200">
                      {order.orderNumber}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-200">
                      <div>
                        <div>{order.userName}</div>
                        <div className="text-xs text-slate-400">{order.userEmail}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-400">
                      {new Date(order.createdAt).toLocaleString('zh-CN')}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-200">
                      {order.itemCount} 件
                    </td>
                    <td className="px-4 py-3 text-sm text-green-400 font-semibold">
                      ${order.totalAmount.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-sm text-right">
                      <button
                        onClick={() => handleViewDetail(order.id)}
                        className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                      >
                        查看详情
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 空状态 */}
          {orders.length === 0 && (
            <div className="text-center py-12">
              <p className="text-slate-400">暂无订单</p>
            </div>
          )}

          {/* 分页 */}
          {totalPages > 1 && (
            <div className="px-4 py-3 bg-slate-900 border-t border-slate-700 flex justify-between items-center">
              <div className="text-sm text-slate-400">
                第 {currentPage} 页，共 {totalPages} 页
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handlePrevPage}
                  disabled={currentPage === 1}
                  className="px-4 py-2 bg-slate-700 text-white rounded hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  上一页
                </button>
                <button
                  onClick={handleNextPage}
                  disabled={currentPage >= totalPages}
                  className="px-4 py-2 bg-slate-700 text-white rounded hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  下一页
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 订单详情弹窗 */}
      {showDetail && selectedOrder && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 max-w-3xl w-full mx-4 border border-slate-700 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">订单详情</h2>
              <button
                onClick={() => setShowDetail(false)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              {/* 订单基本信息 */}
              <div className="grid grid-cols-2 gap-4 pb-4 border-b border-slate-700">
                <div>
                  <div className="text-sm text-slate-400">订单号</div>
                  <div className="font-mono text-slate-200">{selectedOrder.orderNumber}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">下单时间</div>
                  <div className="text-slate-200">
                    {new Date(selectedOrder.createdAt).toLocaleString('zh-CN')}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">用户姓名</div>
                  <div className="text-slate-200">{selectedOrder.userName}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">用户邮箱</div>
                  <div className="text-slate-200">{selectedOrder.userEmail}</div>
                </div>
              </div>

              {/* 订单项列表 */}
              <div>
                <h3 className="text-lg font-semibold mb-3">订单商品</h3>
                <div className="space-y-2">
                  {selectedOrder.items.map((item, index) => (
                    <div
                      key={index}
                      className="flex justify-between items-center p-3 bg-slate-900 rounded"
                    >
                      <div className="flex-1">
                        <div className="text-slate-200">{item.phoneTitle}</div>
                        <div className="text-sm text-slate-400">
                          单价: ${item.priceAtPurchase.toFixed(2)} × {item.quantity}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-slate-200 font-semibold">
                          ${item.subtotal.toFixed(2)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 订单总计 */}
              <div className="pt-4 border-t border-slate-700">
                <div className="flex justify-between items-center">
                  <span className="text-lg font-semibold">订单总额</span>
                  <span className="text-2xl font-bold text-green-400">
                    ${selectedOrder.totalAmount.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getAdminToken } from '@/lib/authStorage';
import {
  getAdminPhones,
  toggleAdminPhoneDisabled,
  deleteAdminPhone,
  updateAdminPhone
} from '@/lib/adminPhoneApi';
import type {
  PhoneManagementResponse,
  UpdatePhoneRequest
} from '@/types/admin';

export default function AdminListingsPage(): JSX.Element {
  const { admin, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [phones, setPhones] = useState<PhoneManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [editingPhone, setEditingPhone] = useState<PhoneManagementResponse | null>(null);
  const [showEdit, setShowEdit] = useState(false);
  const pageSize = 10;

  // 检查管理员登录状态
  useEffect(() => {
    if (!authLoading && !admin) {
      router.push('/login?mode=admin&redirect=/admin/listings');
    }
  }, [admin, authLoading, router]);

  // 加载挂牌列表
  const loadPhones = async (page: number = 1) => {
    const token = getAdminToken();
    if (!token || !admin) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getAdminPhones(token, { page, pageSize });
      setPhones(response.content);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotal(response.totalItems);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载挂牌列表失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (admin) {
      loadPhones(1);
    }
  }, [admin]);

  // 切换挂牌状态
  const handleToggleStatus = async (phoneId: string) => {
    const token = getAdminToken();
    if (!token) return;

    try {
      await toggleAdminPhoneDisabled(token, phoneId);
      await loadPhones(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '操作失败';
      alert(message);
    }
  };

  // 删除挂牌
  const handleDelete = async (phoneId: string, phoneTitle: string) => {
    if (!confirm(`确定要删除挂牌 "${phoneTitle}" 吗？此操作不可撤销。`)) {
      return;
    }

    const token = getAdminToken();
    if (!token) return;

    try {
      await deleteAdminPhone(token, phoneId);
      await loadPhones(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '删除失败';
      alert(message);
    }
  };

  // 打开编辑弹窗
  const handleEdit = (phone: PhoneManagementResponse) => {
    setEditingPhone(phone);
    setShowEdit(true);
  };

  // 保存编辑
  const handleSaveEdit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingPhone) return;

    const token = getAdminToken();
    if (!token) return;

    const formData = new FormData(e.currentTarget);
    const updateData: UpdatePhoneRequest = {
      price: parseFloat(formData.get('price') as string),
      stock: parseInt(formData.get('stock') as string, 10)
    };

    try {
      await updateAdminPhone(token, editingPhone.id, updateData);
      setShowEdit(false);
      setEditingPhone(null);
      await loadPhones(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '更新失败';
      alert(message);
    }
  };

  // 分页控制
  const handlePrevPage = () => {
    if (currentPage > 1) {
      loadPhones(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      loadPhones(currentPage + 1);
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
        <h1 className="text-2xl font-semibold">挂牌管理</h1>
        <div className="text-sm text-slate-400">
          共 {total} 个挂牌
        </div>
      </div>

      {/* Error 状态 */}
      {error && (
        <div className="rounded-lg bg-red-500/10 border border-red-500/20 p-4">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => loadPhones(currentPage)}
            className="mt-2 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            重试
          </button>
        </div>
      )}

      {/* 挂牌列表 */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-400">正在加载挂牌列表...</p>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-900 border-b border-slate-700">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">标题</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">品牌</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">价格</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">库存</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">卖家</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">评分</th>
                  <th className="px-4 py-3 text-right text-sm font-semibold text-slate-300">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {phones.map((phone) => (
                  <tr key={phone.id} className="hover:bg-slate-700/50">
                    <td className="px-4 py-3 text-sm text-slate-200 max-w-xs truncate">
                      {phone.title}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-200">{phone.brand}</td>
                    <td className="px-4 py-3 text-sm text-slate-200">${phone.price.toFixed(2)}</td>
                    <td className="px-4 py-3 text-sm text-slate-200">{phone.stock}</td>
                    <td className="px-4 py-3 text-sm text-slate-200">
                      <div>
                        <div>{phone.sellerName}</div>
                        <div className="text-xs text-slate-400">{phone.sellerEmail}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {phone.isDisabled ? (
                        <span className="px-2 py-1 text-xs bg-red-500/20 text-red-400 rounded">
                          已禁用
                        </span>
                      ) : (
                        <span className="px-2 py-1 text-xs bg-green-500/20 text-green-400 rounded">
                          启用中
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-200">
                      <div>
                        <div>{phone.averageRating.toFixed(1)} ⭐</div>
                        <div className="text-xs text-slate-400">({phone.reviewCount} 评论)</div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleEdit(phone)}
                          className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                        >
                          编辑
                        </button>
                        <button
                          onClick={() => handleToggleStatus(phone.id)}
                          className={`px-3 py-1 rounded ${
                            phone.isDisabled
                              ? 'bg-green-500 hover:bg-green-600'
                              : 'bg-yellow-500 hover:bg-yellow-600'
                          } text-white`}
                        >
                          {phone.isDisabled ? '启用' : '禁用'}
                        </button>
                        <button
                          onClick={() => handleDelete(phone.id, phone.title)}
                          className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600"
                        >
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

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

      {/* 编辑弹窗 */}
      {showEdit && editingPhone && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 max-w-md w-full mx-4 border border-slate-700">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">编辑挂牌</h2>
              <button
                onClick={() => {
                  setShowEdit(false);
                  setEditingPhone(null);
                }}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleSaveEdit} className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">标题</label>
                <input
                  type="text"
                  value={editingPhone.title}
                  disabled
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-400"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">价格</label>
                <input
                  type="number"
                  name="price"
                  step="0.01"
                  min="0"
                  defaultValue={editingPhone.price}
                  required
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">库存</label>
                <input
                  type="number"
                  name="stock"
                  min="0"
                  defaultValue={editingPhone.stock}
                  required
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white"
                />
              </div>
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => {
                    setShowEdit(false);
                    setEditingPhone(null);
                  }}
                  className="px-4 py-2 bg-slate-600 text-white rounded hover:bg-slate-500"
                >
                  取消
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                  保存
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

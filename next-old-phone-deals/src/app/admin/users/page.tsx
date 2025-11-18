'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getAdminToken } from '@/lib/authStorage';
import {
  getAdminUsers,
  toggleUserDisabled,
  deleteUser,
  getUserDetail
} from '@/lib/adminUserApi';
import type {
  UserManagementResponse,
  UserDetailResponse
} from '@/types/admin';

export default function AdminUsersPage(): JSX.Element {
  const { admin, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [users, setUsers] = useState<UserManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [selectedUser, setSelectedUser] = useState<UserDetailResponse | null>(null);
  const [showDetail, setShowDetail] = useState(false);
  const pageSize = 10;

  // 检查管理员登录状态
  useEffect(() => {
    if (!authLoading && !admin) {
      router.push('/login?mode=admin&redirect=/admin/users');
    }
  }, [admin, authLoading, router]);

  // 加载用户列表
  const loadUsers = async (page: number = 0) => {
    const token = getAdminToken();
    if (!token || !admin) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getAdminUsers(token, { page, pageSize });
      setUsers(response.content);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotal(response.total);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载用户列表失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (admin) {
      loadUsers(0);
    }
  }, [admin]);

  // 切换用户状态
  const handleToggleStatus = async (userId: string) => {
    const token = getAdminToken();
    if (!token) return;

    try {
      await toggleUserDisabled(token, userId);
      await loadUsers(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '操作失败';
      alert(message);
    }
  };

  // 删除用户
  const handleDelete = async (userId: string, userEmail: string) => {
    if (!confirm(`确定要删除用户 ${userEmail} 吗？此操作不可撤销。`)) {
      return;
    }

    const token = getAdminToken();
    if (!token) return;

    try {
      await deleteUser(token, userId);
      await loadUsers(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '删除失败';
      alert(message);
    }
  };

  // 查看用户详情
  const handleViewDetail = async (userId: string) => {
    const token = getAdminToken();
    if (!token) return;

    try {
      const detail = await getUserDetail(token, userId);
      setSelectedUser(detail);
      setShowDetail(true);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载详情失败';
      alert(message);
    }
  };

  // 分页控制
  const handlePrevPage = () => {
    if (currentPage > 0) {
      loadUsers(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages - 1) {
      loadUsers(currentPage + 1);
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
        <h1 className="text-2xl font-semibold">用户管理</h1>
        <div className="text-sm text-slate-400">
          共 {total} 个用户
        </div>
      </div>

      {/* Error 状态 */}
      {error && (
        <div className="rounded-lg bg-red-500/10 border border-red-500/20 p-4">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => loadUsers(currentPage)}
            className="mt-2 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            重试
          </button>
        </div>
      )}

      {/* 用户列表 */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-400">正在加载用户列表...</p>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-900 border-b border-slate-700">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">邮箱</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">姓名</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">注册时间</th>
                  <th className="px-4 py-3 text-left text-sm font-semibold text-slate-300">最后登录</th>
                  <th className="px-4 py-3 text-right text-sm font-semibold text-slate-300">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-700/50">
                    <td className="px-4 py-3 text-sm text-slate-200">{user.email}</td>
                    <td className="px-4 py-3 text-sm text-slate-200">
                      {user.firstName} {user.lastName}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex items-center gap-2">
                        {user.isDisabled ? (
                          <span className="px-2 py-1 text-xs bg-red-500/20 text-red-400 rounded">
                            已禁用
                          </span>
                        ) : (
                          <span className="px-2 py-1 text-xs bg-green-500/20 text-green-400 rounded">
                            正常
                          </span>
                        )}
                        {user.isEmailVerified ? (
                          <span className="px-2 py-1 text-xs bg-blue-500/20 text-blue-400 rounded">
                            已验证
                          </span>
                        ) : (
                          <span className="px-2 py-1 text-xs bg-yellow-500/20 text-yellow-400 rounded">
                            未验证
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-400">
                      {new Date(user.createdAt).toLocaleDateString('zh-CN')}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-400">
                      {user.lastLoginAt
                        ? new Date(user.lastLoginAt).toLocaleDateString('zh-CN')
                        : '-'}
                    </td>
                    <td className="px-4 py-3 text-sm text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleViewDetail(user.id)}
                          className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                        >
                          详情
                        </button>
                        <button
                          onClick={() => handleToggleStatus(user.id)}
                          className={`px-3 py-1 rounded ${
                            user.isDisabled
                              ? 'bg-green-500 hover:bg-green-600'
                              : 'bg-yellow-500 hover:bg-yellow-600'
                          } text-white`}
                        >
                          {user.isDisabled ? '启用' : '禁用'}
                        </button>
                        <button
                          onClick={() => handleDelete(user.id, user.email)}
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
                第 {currentPage + 1} 页，共 {totalPages} 页
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handlePrevPage}
                  disabled={currentPage === 0}
                  className="px-4 py-2 bg-slate-700 text-white rounded hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  上一页
                </button>
                <button
                  onClick={handleNextPage}
                  disabled={currentPage >= totalPages - 1}
                  className="px-4 py-2 bg-slate-700 text-white rounded hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  下一页
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 用户详情弹窗 */}
      {showDetail && selectedUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 max-w-2xl w-full mx-4 border border-slate-700">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">用户详情</h2>
              <button
                onClick={() => setShowDetail(false)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-slate-400">邮箱</div>
                  <div className="text-slate-200">{selectedUser.email}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">姓名</div>
                  <div className="text-slate-200">
                    {selectedUser.firstName} {selectedUser.lastName}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">邮箱验证</div>
                  <div className="text-slate-200">
                    {selectedUser.isEmailVerified ? '已验证' : '未验证'}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">账号状态</div>
                  <div className="text-slate-200">
                    {selectedUser.isDisabled ? '已禁用' : '正常'}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">注册时间</div>
                  <div className="text-slate-200">
                    {new Date(selectedUser.createdAt).toLocaleString('zh-CN')}
                  </div>
                </div>
                <div>
                  <div className="text-sm text-slate-400">最后登录</div>
                  <div className="text-slate-200">
                    {selectedUser.lastLoginAt
                      ? new Date(selectedUser.lastLoginAt).toLocaleString('zh-CN')
                      : '从未登录'}
                  </div>
                </div>
              </div>
              <div className="pt-4 border-t border-slate-700">
                <h3 className="text-lg font-semibold mb-2">统计信息</h3>
                <div className="grid grid-cols-3 gap-4">
                  <div className="bg-slate-900 rounded p-3">
                    <div className="text-sm text-slate-400">挂牌数量</div>
                    <div className="text-2xl font-bold text-blue-400">
                      {selectedUser.listingsCount}
                    </div>
                  </div>
                  <div className="bg-slate-900 rounded p-3">
                    <div className="text-sm text-slate-400">评论数量</div>
                    <div className="text-2xl font-bold text-green-400">
                      {selectedUser.reviewsCount}
                    </div>
                  </div>
                  <div className="bg-slate-900 rounded p-3">
                    <div className="text-sm text-slate-400">订单数量</div>
                    <div className="text-2xl font-bold text-purple-400">
                      {selectedUser.ordersCount}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
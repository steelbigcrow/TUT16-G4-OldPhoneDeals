'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/components/auth/AuthProvider';
import { getAdminToken } from '@/lib/authStorage';
import {
  getAdminReviews,
  toggleReviewVisibility,
  deleteReview
} from '@/lib/adminReviewApi';
import type { ReviewManagementResponse } from '@/types/admin';

export default function AdminReviewsPage(): JSX.Element {
  const { admin, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [reviews, setReviews] = useState<ReviewManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const pageSize = 10;

  // 检查管理员登录状态
  useEffect(() => {
    if (!authLoading && !admin) {
      router.push('/login?mode=admin&redirect=/admin/reviews');
    }
  }, [admin, authLoading, router]);

  // 加载评论列表
  const loadReviews = async (page: number = 0) => {
    const token = getAdminToken();
    if (!token || !admin) return;

    try {
      setLoading(true);
      setError(null);
      const response = await getAdminReviews(token, { page, pageSize });
      setReviews(response.content);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotal(response.total);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载评论列表失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (admin) {
      loadReviews(0);
    }
  }, [admin]);

  // 切换评论可见性
  const handleToggleVisibility = async (phoneId: string, reviewId: string) => {
    const token = getAdminToken();
    if (!token) return;

    try {
      await toggleReviewVisibility(token, phoneId, reviewId);
      await loadReviews(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '操作失败';
      alert(message);
    }
  };

  // 删除评论
  const handleDelete = async (
    phoneId: string,
    reviewId: string,
    phoneTitle: string
  ) => {
    if (!confirm(`确定要删除该评论吗？此操作不可撤销。`)) {
      return;
    }

    const token = getAdminToken();
    if (!token) return;

    try {
      await deleteReview(token, phoneId, reviewId);
      await loadReviews(currentPage);
    } catch (err) {
      const message = err instanceof Error ? err.message : '删除失败';
      alert(message);
    }
  };

  // 分页控制
  const handlePrevPage = () => {
    if (currentPage > 0) {
      loadReviews(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages - 1) {
      loadReviews(currentPage + 1);
    }
  };

  // 渲染星级
  const renderStars = (rating: number) => {
    return '⭐'.repeat(rating) + '☆'.repeat(5 - rating);
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
        <h1 className="text-2xl font-semibold">评论管理</h1>
        <div className="text-sm text-slate-400">
          共 {total} 条评论
        </div>
      </div>

      {/* Error 状态 */}
      {error && (
        <div className="rounded-lg bg-red-500/10 border border-red-500/20 p-4">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => loadReviews(currentPage)}
            className="mt-2 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            重试
          </button>
        </div>
      )}

      {/* 评论列表 */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-400">正在加载评论列表...</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div
              key={review.id}
              className="bg-slate-800 rounded-lg p-6 border border-slate-700"
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-lg font-semibold text-slate-200">
                      {review.phoneTitle}
                    </h3>
                    {review.isHidden ? (
                      <span className="px-2 py-1 text-xs bg-red-500/20 text-red-400 rounded">
                        已隐藏
                      </span>
                    ) : (
                      <span className="px-2 py-1 text-xs bg-green-500/20 text-green-400 rounded">
                        可见
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-4 text-sm text-slate-400">
                    <span>评论者：{review.userName}</span>
                    <span>({review.userEmail})</span>
                    <span>•</span>
                    <span>{new Date(review.createdAt).toLocaleString('zh-CN')}</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() =>
                      handleToggleVisibility(review.phoneId, review.id)
                    }
                    className={`px-3 py-1 rounded text-sm ${
                      review.isHidden
                        ? 'bg-green-500 hover:bg-green-600'
                        : 'bg-yellow-500 hover:bg-yellow-600'
                    } text-white`}
                  >
                    {review.isHidden ? '显示' : '隐藏'}
                  </button>
                  <button
                    onClick={() =>
                      handleDelete(review.phoneId, review.id, review.phoneTitle)
                    }
                    className="px-3 py-1 bg-red-500 text-white rounded text-sm hover:bg-red-600"
                  >
                    删除
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-slate-400">评分：</span>
                  <span className="text-lg">{renderStars(review.rating)}</span>
                  <span className="text-sm text-slate-400">
                    ({review.rating}/5)
                  </span>
                </div>
                <div>
                  <span className="text-sm text-slate-400">评论内容：</span>
                  <p className="mt-1 text-slate-200 whitespace-pre-wrap">
                    {review.comment}
                  </p>
                </div>
              </div>
            </div>
          ))}

          {/* 空状态 */}
          {reviews.length === 0 && (
            <div className="text-center py-12">
              <p className="text-slate-400">暂无评论</p>
            </div>
          )}
        </div>
      )}

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center bg-slate-800 rounded-lg p-4 border border-slate-700">
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
  );
}
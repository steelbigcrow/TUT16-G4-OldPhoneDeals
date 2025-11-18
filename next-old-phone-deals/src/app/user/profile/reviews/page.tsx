'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/components/auth/AuthProvider';
import { getReviewsBySeller } from '@/lib/reviewApi';
import type { SellerReviewResponse } from '@/types/review';

export default function UserReviewsPage(): JSX.Element {
  const router = useRouter();
  const { user, userToken, isLoading } = useAuth();
  const [reviews, setReviews] = useState<SellerReviewResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterRating, setFilterRating] = useState<number | ''>('');
  const [searchPhone, setSearchPhone] = useState('');

  useEffect(() => {
    if (isLoading) return;

    if (!user || !userToken) {
      router.push('/login?redirect=/user/profile/reviews');
      return;
    }

    fetchReviews();
  }, [user, userToken, isLoading, router]);

  const fetchReviews = async () => {
    if (!userToken) return;

    try {
      setLoading(true);
      setError('');
      const data = await getReviewsBySeller(userToken);
      setReviews(data);
    } catch (err) {
      console.error('获取评论列表失败:', err);
      setError('获取评论列表失败，请稍后重试');
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

  // 过滤评论
  const filteredReviews = reviews.filter(review => {
    if (filterRating && review.rating !== filterRating) return false;
    if (searchPhone && !review.phoneTitle.toLowerCase().includes(searchPhone.toLowerCase())) {
      return false;
    }
    return true;
  });

  // 计算统计数据
  const avgRating = reviews.length > 0
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
    : '0.0';
  const ratingCounts = [1, 2, 3, 4, 5].map(rating => 
    reviews.filter(r => r.rating === rating).length
  );

  // 渲染星级
  const renderStars = (rating: number) => {
    return (
      <div className="flex items-center">
        {[1, 2, 3, 4, 5].map(star => (
          <svg
            key={star}
            className={`h-5 w-5 ${
              star <= rating ? 'text-yellow-400' : 'text-gray-300'
            }`}
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
          </svg>
        ))}
        <span className="ml-2 text-sm text-gray-600">({rating}星)</span>
      </div>
    );
  };

  return (
    <section className="space-y-6">
      <h1 className="text-3xl font-bold">收到的评论</h1>

      {/* 统计概览 */}
      <div className="bg-white shadow rounded-lg p-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600">{reviews.length}</div>
            <div className="text-sm text-gray-600 mt-1">总评论数</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-yellow-600">{avgRating}</div>
            <div className="text-sm text-gray-600 mt-1">平均评分</div>
          </div>
          <div className="text-center">
            <div className="space-y-1">
              {[5, 4, 3, 2, 1].map((rating, idx) => (
                <div key={rating} className="flex items-center text-xs">
                  <span className="w-12">{rating}星</span>
                  <div className="flex-1 bg-gray-200 rounded-full h-2 mx-2">
                    <div
                      className="bg-yellow-400 h-2 rounded-full"
                      style={{
                        width: reviews.length > 0
                          ? `${(ratingCounts[rating - 1] / reviews.length) * 100}%`
                          : '0%'
                      }}
                    />
                  </div>
                  <span className="w-8 text-right">{ratingCounts[rating - 1]}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 筛选栏 */}
      <div className="bg-white shadow rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              按评分筛选
            </label>
            <select
              value={filterRating}
              onChange={(e) => setFilterRating(e.target.value ? Number(e.target.value) : '')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">全部评分</option>
              <option value="5">5星</option>
              <option value="4">4星</option>
              <option value="3">3星</option>
              <option value="2">2星</option>
              <option value="1">1星</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              按商品搜索
            </label>
            <input
              type="text"
              placeholder="输入商品标题..."
              value={searchPhone}
              onChange={(e) => setSearchPhone(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      </div>

      {/* 评论列表 */}
      {filteredReviews.length === 0 ? (
        <div className="bg-white shadow rounded-lg p-8 text-center text-gray-500">
          {reviews.length === 0 ? '暂无评论' : '没有符合条件的评论'}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredReviews.map((review) => (
            <div key={review.id} className="bg-white shadow rounded-lg p-6">
              <div className="flex justify-between items-start mb-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <Link
                      href={`/phones/${review.phoneId}`}
                      className="text-lg font-semibold text-blue-600 hover:text-blue-800"
                    >
                      {review.phoneTitle}
                    </Link>
                    {review.isHidden && (
                      <span className="px-2 py-1 text-xs bg-gray-200 text-gray-600 rounded">
                        已隐藏
                      </span>
                    )}
                  </div>
                  {renderStars(review.rating)}
                </div>
                <div className="text-sm text-gray-500">
                  {new Date(review.createdAt).toLocaleDateString('zh-CN')}
                </div>
              </div>

              <div className="mb-3">
                <p className="text-gray-700 whitespace-pre-wrap">{review.comment}</p>
              </div>

              <div className="flex items-center text-sm text-gray-500">
                <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                评论者：{review.userName}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="text-sm text-gray-500">
        共 {filteredReviews.length} 条评论
        {filteredReviews.length !== reviews.length && ` (从 ${reviews.length} 条筛选)`}
      </div>
    </section>
  );
}
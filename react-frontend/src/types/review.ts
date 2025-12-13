import type { IsoDateTimeString } from './api';

export type ReviewResponse = {
  id: string;
  reviewerId: string;
  rating: number | null;
  comment: string;
  isHidden: boolean | null;
  reviewer: string;
  createdAt: IsoDateTimeString | null;
};

export type ReviewPageResponse = {
  reviews: ReviewResponse[];
  totalReviews: number;
  /** 1-based page number returned by backend. */
  currentPage: number;
  totalPages: number;
};

export type ReviewManagementResponse = {
  reviewId: string;
  phoneId: string;
  phoneTitle: string;
  reviewerId: string;
  reviewerName: string;
  rating: number | null;
  comment: string;
  isHidden: boolean | null;
  createdAt: IsoDateTimeString | null;
};

export type PhoneReviewListSuccessResponse = {
  success: true;
  message?: string;
  total: number;
  /** 1-based page number returned by backend. */
  page: number;
  limit: number;
  reviews: ReviewManagementResponse[];
};

export type PhoneReviewListErrorResponse = {
  success: false;
  message: string;
};

export type PhoneReviewListResponse =
  | PhoneReviewListSuccessResponse
  | PhoneReviewListErrorResponse;

export type SellerReviewResponse = {
  reviewId: string;
  phoneId: string;
  phoneTitle: string;
  reviewerId: string;
  reviewerName: string;
  rating: number | null;
  comment: string;
  isHidden: boolean | null;
  createdAt: IsoDateTimeString | null;
};
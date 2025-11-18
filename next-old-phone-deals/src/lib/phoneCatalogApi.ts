import { apiGet } from './apiClient';
import type {
  CatalogPhone,
  CatalogSearchResponse,
  SpecialPhoneCategory
} from '@/types/phone';

type RawReview = {
  rating?: number;
};

type RawSeller = {
  _id?: string;
  id?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
};

type RawPhone = {
  _id?: string;
  id?: string;
  title: string;
  brand: string;
  price: number;
  stock: number;
  image?: string;
  imageUrl?: string;
  averageRating?: number;
  reviewCount?: number;
  reviews?: RawReview[];
  seller?: RawSeller;
  salesCount?: number;
  isDisabled?: boolean;
};

type RawSearchResponse = {
  phones?: RawPhone[];
  totalPages?: number;
  currentPage?: number;
  total?: number;
  message?: string;
};

const DEFAULT_MEDIA_BASE =
  process.env.NEXT_PUBLIC_FILES_BASE_URL ?? 'http://localhost:3000';

function ensureAbsoluteUrl(path?: string): string | undefined {
  if (!path) {
    return undefined;
  }

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  if (path.startsWith('/')) {
    return `${DEFAULT_MEDIA_BASE}${path}`;
  }

  return `${DEFAULT_MEDIA_BASE}/${path}`;
}

function calculateAverageRating(reviews?: RawReview[]): number | undefined {
  if (!reviews || reviews.length === 0) {
    return undefined;
  }

  const total = reviews.reduce(
    (sum, review) => sum + (review.rating ?? 0),
    0
  );
  return Number((total / reviews.length).toFixed(1));
}

function normalizePhone(raw: RawPhone): CatalogPhone {
  const id = raw.id || raw._id;
  if (!id) {
    throw new Error('Phone payload is missing an id');
  }

  const sellerNameParts = [raw.seller?.firstName, raw.seller?.lastName]
    .filter(Boolean)
    .join(' ')
    .trim();

  const sellerName =
    raw.seller?.username ??
    (sellerNameParts.length > 0 ? sellerNameParts : undefined);

  const reviewCount =
    typeof raw.reviewCount === 'number'
      ? raw.reviewCount
      : raw.reviews
        ? raw.reviews.length
        : undefined;

  const averageRating =
    typeof raw.averageRating === 'number'
      ? Number(raw.averageRating.toFixed(1))
      : calculateAverageRating(raw.reviews);

  const imageUrl = ensureAbsoluteUrl(raw.imageUrl || raw.image);

  return {
    id,
    title: raw.title,
    brand: raw.brand,
    price: raw.price,
    stock: raw.stock,
    imageUrl,
    averageRating,
    reviewCount,
    sellerName,
    salesCount: raw.salesCount,
    isDisabled: raw.isDisabled ?? false
  };
}

export type SearchPhonesParams = {
  search?: string;
  brand?: string;
  maxPrice?: number | null;
  page?: number;
  limit?: number;
};

function buildSearchQuery(params: SearchPhonesParams): URLSearchParams {
  const query = new URLSearchParams();
  const page = params.page && params.page > 0 ? params.page : 1;
  const limit = params.limit && params.limit > 0 ? params.limit : 10;

  query.set('page', String(page));
  query.set('limit', String(limit));

  if (params.search) {
    query.set('search', params.search);
  }

  if (params.brand) {
    query.set('brand', params.brand);
  }

  if (typeof params.maxPrice === 'number' && !Number.isNaN(params.maxPrice)) {
    query.set('maxPrice', String(Math.max(0, params.maxPrice)));
  }

  return query;
}

export async function fetchSpecialPhones(
  special: SpecialPhoneCategory
): Promise<CatalogPhone[]> {
  const response = await apiGet<unknown>(`/phones?special=${special}`);

  if (!Array.isArray(response)) {
    const message =
      typeof response === 'object' && response && 'message' in response
        ? String((response as { message?: unknown }).message)
        : `Unexpected response when fetching ${special} phones`;
    throw new Error(message || 'Failed to load featured phones');
  }

  return (response as RawPhone[]).map(normalizePhone);
}

export async function searchPhones(
  params: SearchPhonesParams = {}
): Promise<CatalogSearchResponse> {
  const query = buildSearchQuery(params);
  const response = await apiGet<RawSearchResponse>(`/phones?${query.toString()}`);

  if (!response || !Array.isArray(response.phones)) {
    throw new Error(
      response?.message || 'Invalid response when searching for phones'
    );
  }

  return {
    phones: response.phones.map(normalizePhone),
    totalPages: response.totalPages ?? 1,
    currentPage:
      response.currentPage ??
      (params.page && params.page > 0 ? params.page : 1),
    total: response.total ?? response.phones.length
  };
}

// Exporting for testing
export const __internal = {
  ensureAbsoluteUrl,
  normalizePhone,
  buildSearchQuery
};

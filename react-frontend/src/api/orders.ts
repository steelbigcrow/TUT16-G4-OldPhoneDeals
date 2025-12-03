import client from './client';
import type { ApiResponse } from '../types/api';
import type { Order, OrderPaginationMeta } from '../types/order';

export type OrdersPage = {
  items: Order[];
  pagination: OrderPaginationMeta;
};

export const fetchUserOrders = async (
  page = 1,
  pageSize = 10
): Promise<OrdersPage> => {
  const response = await client.get('/orders', {
    params: { page, pageSize }
  });

  const raw = response.data;

  // Spring Boot contract: ApiResponse<{ items, pagination }>
  if (raw && typeof raw === 'object' && 'success' in raw) {
    const payload = raw as ApiResponse<any>;

    if (!payload.success) {
      throw new Error(payload.message || 'Failed to load orders');
    }

    const data = payload.data as
      | { items?: Order[]; pagination?: Partial<OrderPaginationMeta> }
      | undefined;

    if (data && Array.isArray(data.items) && data.pagination) {
      const { items, pagination } = data;
      const currentPage =
        typeof pagination.currentPage === 'number' && pagination.currentPage > 0
          ? pagination.currentPage
          : page;
      const size =
        typeof pagination.pageSize === 'number' && pagination.pageSize > 0
          ? pagination.pageSize
          : pageSize;
      const totalItems =
        typeof pagination.totalItems === 'number' && pagination.totalItems >= 0
          ? pagination.totalItems
          : items.length;
      const totalPages =
        typeof pagination.totalPages === 'number' && pagination.totalPages > 0
          ? pagination.totalPages
          : Math.max(1, Math.ceil(totalItems / size));

      return {
        items,
        pagination: {
          currentPage,
          pageSize: size,
          totalItems,
          totalPages
        }
      };
    }
  }

  // Legacy Node.js contract: { orders, total, totalPages, currentPage }
  if (raw && typeof raw === 'object' && Array.isArray((raw as any).orders)) {
    const legacy = raw as any;
    const items = (legacy.orders || []) as Order[];
    const currentPage =
      typeof legacy.currentPage === 'number' && legacy.currentPage > 0
        ? legacy.currentPage
        : page;
    const totalItems =
      typeof legacy.total === 'number' && legacy.total >= 0
        ? legacy.total
        : items.length;
    const size = pageSize > 0 ? pageSize : 10;
    const totalPages =
      typeof legacy.totalPages === 'number' && legacy.totalPages > 0
        ? legacy.totalPages
        : Math.max(1, Math.ceil(totalItems / size));

    return {
      items,
      pagination: {
        currentPage,
        pageSize: size,
        totalItems,
        totalPages
      }
    };
  }

  throw new Error('Unexpected orders response shape');
};

export type IsoDateTimeString = string;

export type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data?: T;
};

export type PageResponse<T> = {
  content: T[];
  /** 1-based page number returned by backend. */
  currentPage: number;
  itemsPerPage: number;
  totalPages: number;
  totalItems: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type OrderPagination = {
  /** 1-based page number returned by backend. */
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
};

export type OrderPageResponse<TOrder> = {
  items: TOrder[];
  pagination: OrderPagination;
};

export type PhonesListData<TPhone> = {
  phones: TPhone[];
  /** 1-based page number returned by backend. */
  currentPage: number;
  totalPages: number;
  total: number;
};
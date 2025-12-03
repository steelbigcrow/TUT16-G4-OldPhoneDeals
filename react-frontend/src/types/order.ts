export type OrderItem = {
  phoneId: string;
  title: string;
  quantity: number;
  price: number;
};

export type OrderAddress = {
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
};

export type Order = {
  id?: string;
  _id?: string;
  userId?: string;
  items: OrderItem[];
  totalAmount: number;
  address?: OrderAddress | null;
  createdAt: string;
};

export type OrderPaginationMeta = {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
};

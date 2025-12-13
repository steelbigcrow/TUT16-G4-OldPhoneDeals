import type { IsoDateTimeString } from './api';

export type OrderItemResponse = {
  phoneId: string;
  title: string;
  quantity: number | null;
  price: number | null;
};

export type OrderAddressInfo = {
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
};

export type OrderResponse = {
  id: string;
  userId: string;
  items: OrderItemResponse[];
  totalAmount: number | null;
  address: OrderAddressInfo | null;
  createdAt: IsoDateTimeString | null;
};
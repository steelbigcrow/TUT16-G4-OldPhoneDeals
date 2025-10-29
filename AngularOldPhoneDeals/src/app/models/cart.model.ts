export interface CartItem {
  phoneId: string;
  title: string;
  price: number;
  quantity: number;
  averageRating?: number;
  reviewCount?: number;
  seller?: {
    firstName: string;
    lastName: string;
    _id: string;
  };
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  updatedAt?: string;
}

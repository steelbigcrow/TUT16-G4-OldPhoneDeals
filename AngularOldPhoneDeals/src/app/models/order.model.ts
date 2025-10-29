import { CartItem } from './cart.model';
import { Address } from './user.model';

export interface Order {
  id: string;
  userId: string;
  items: CartItem[];
  totalAmount: number;
  address: Address;
  createdAt: string;
}

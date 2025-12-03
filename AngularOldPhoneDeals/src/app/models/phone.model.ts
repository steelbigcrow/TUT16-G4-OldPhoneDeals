import { Review } from './review.model';
import { User } from './user.model';

export interface Phone {
  id: string;            // backend _id mapped to frontend id
  title: string;
  brand: string;
  image?: string;
  stock: number;
  sellerId?: string;
  seller?: User;         // optional seller object, provided by backend
  price: number;
  reviews?: Review[];
  reviewsCount?: number;
  isDisabled: boolean;
  averageRating?: number; // optional, calculated by frontend or backend
  createdAt?: string | Date;
  updatedAt?: string | Date;
}

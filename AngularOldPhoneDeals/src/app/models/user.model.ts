export interface Address {
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
}

export interface User {
  id: string;             // map _id → id
  firstName: string;
  lastName: string;
  email: string;
  isVerified: boolean;
  isAdmin?: boolean;            // is admin
  isDisabled?: boolean;         // is disabled
  isBan?: boolean;              // is banned
  wishlist: string[];     // phoneId list
  address?: Address;
  lastLogin?: string;           // ISO string
  createdAt?: string;
  updatedAt?: string;
  // other fields
}

export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  lastLogin: Date;
  isDisabled?: boolean;
}

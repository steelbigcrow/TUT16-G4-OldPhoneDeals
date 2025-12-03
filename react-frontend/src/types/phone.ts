export type AdminSeller = {
  _id: string;
  firstName: string;
  lastName: string;
  email: string;
};

export type AdminPhoneDetail = {
  _id: string;
  title: string;
  brand: string;
  image: string;
  price: number;
  stock: number;
  isDisabled: boolean;
  createdAt: string;
  updatedAt: string;
  seller: AdminSeller;
};

export type AdminPhoneListItem = AdminPhoneDetail & {
  reviews?: any[];
};

import type { IsoDateTimeString } from './api';

export type UserResponse = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  isAdmin: boolean | null;
  isVerified: boolean | null;
  isDisabled: boolean | null;
  isBan: boolean | null;
};

export type UserProfileResponse = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  emailVerified: boolean | null;
  createdAt: IsoDateTimeString | null;
  updatedAt: IsoDateTimeString | null;
};

export type AuthUserResponse = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  isAdmin: boolean | null;
  isDisabled: boolean | null;
  isBan: boolean | null;
  isVerified: boolean | null;
};

export type LoginResponse = {
  token: string;
  user: {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
  };
};
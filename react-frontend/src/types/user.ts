export type UserProfile = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type UpdateProfileInput = {
  firstName: string;
  lastName: string;
  email: string;
  /**
   * Optional current password; required when changing email.
   */
  currentPassword?: string;
};

export type ChangePasswordInput = {
  currentPassword: string;
  newPassword: string;
};


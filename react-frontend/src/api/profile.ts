import client from './client';
import type { ApiResponse } from '../types/api';
import type {
  ChangePasswordInput,
  UpdateProfileInput,
  UserProfile
} from '../types/user';

const normalizeProfile = (raw: any): UserProfile => {
  if (!raw || typeof raw !== 'object') {
    throw new Error('Invalid profile payload');
  }

  const id = (raw.id || raw._id || raw.userId || '').toString();

  return {
    id,
    email: String(raw.email || ''),
    firstName: String(raw.firstName || ''),
    lastName: String(raw.lastName || ''),
    emailVerified:
      typeof raw.emailVerified === 'boolean'
        ? raw.emailVerified
        : typeof raw.isVerified === 'boolean'
          ? raw.isVerified
          : undefined,
    createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined
  };
};

const syncProfileToStorage = (profile: UserProfile) => {
  try {
    // Keep user info in localStorage so headers/menus can show fresh data.
    localStorage.setItem('user', JSON.stringify(profile));
  } catch {
    // Ignore storage errors in environments where localStorage is unavailable.
  }
};

export const getProfile = async (): Promise<UserProfile> => {
  const response = await client.get('/profile');
  const raw = response.data;

  // Spring Boot contract: ApiResponse<UserProfile>
  if (raw && typeof raw === 'object' && 'success' in raw) {
    const payload = raw as ApiResponse<any>;

    if (!payload.success) {
      throw new Error(payload.message || 'Failed to load profile');
    }

    if (payload.data) {
      const profile = normalizeProfile(payload.data);
      syncProfileToStorage(profile);
      return profile;
    }

    // Legacy Node-style: { success, user }
    if ((raw as any).user) {
      const profile = normalizeProfile((raw as any).user);
      syncProfileToStorage(profile);
      return profile;
    }
  }

  // Fallback: raw.user without success flag
  if (raw && typeof raw === 'object' && (raw as any).user) {
    const profile = normalizeProfile((raw as any).user);
    syncProfileToStorage(profile);
    return profile;
  }

  throw new Error('Unexpected profile response shape');
};

export const updateProfile = async (
  input: UpdateProfileInput
): Promise<UserProfile> => {
  const payload: UpdateProfileInput = {
    firstName: input.firstName,
    lastName: input.lastName,
    email: input.email,
    currentPassword: input.currentPassword || undefined
  };

  const response = await client.put('/profile', payload);
  const raw = response.data;

  if (raw && typeof raw === 'object' && 'success' in raw) {
    const api = raw as ApiResponse<any>;
    if (!api.success) {
      throw new Error(api.message || 'Failed to update profile');
    }

    if (api.data) {
      const profile = normalizeProfile(api.data);
      syncProfileToStorage(profile);
      return profile;
    }
  }

  throw new Error('Unexpected profile update response shape');
};

export const changePassword = async (
  input: ChangePasswordInput
): Promise<string> => {
  const response = await client.put('/profile/change-password', input);
  const raw = response.data;

  if (raw && typeof raw === 'object' && 'success' in raw) {
    const api = raw as ApiResponse<null>;
    if (!api.success) {
      throw new Error(api.message || 'Failed to change password');
    }
    return api.message || 'Password changed successfully';
  }

  throw new Error('Unexpected change password response shape');
};


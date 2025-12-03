import client from './client';
import { AdminPhoneDetail, AdminPhoneListItem } from '../types/phone';

const getAdminId = () => {
  try {
    const stored = localStorage.getItem('admin');
    if (!stored) return undefined;
    const admin = JSON.parse(stored);
    return admin?.id || admin?._id;
  } catch {
    return undefined;
  }
};

export type AdminPhonesResponse = {
  success: boolean;
  phones: AdminPhoneListItem[];
  total: number;
  page: number;
  limit: number;
  message?: string;
};

export const fetchAdminPhones = async (page = 1, limit = 10): Promise<AdminPhonesResponse> => {
  const response = await client.get('/admin/phones', {
    params: { page, limit }
  });

  if (!response.data?.success) {
    throw new Error(response.data?.message || 'Failed to load admin phones');
  }

  return {
    success: true,
    phones: response.data.phones || [],
    total: response.data.total || 0,
    page: response.data.page || page,
    limit: response.data.limit || limit
  };
};

export const fetchAdminPhoneDetail = async (phoneId: string): Promise<AdminPhoneDetail> => {
  if (!phoneId) {
    throw new Error('Phone id is required');
  }

  const response = await client.get(`/admin/phones/${phoneId}`);

  if (!response.data?.success || !response.data?.phone) {
    throw new Error(response.data?.message || 'Failed to fetch phone detail');
  }

  return response.data.phone as AdminPhoneDetail;
};

export const updateAdminPhoneStatus = async (phoneId: string, isDisabled: boolean) => {
  const adminId = getAdminId();
  if (!adminId) {
    throw new Error('Admin session missing. Please log in again.');
  }

  const response = await client.patch(`/admin/phones/${phoneId}`, {
    isDisabled,
    adminId
  });

  if (!response.data?.success) {
    throw new Error(response.data?.message || 'Failed to update phone status');
  }

  return response.data;
};

export const deleteAdminPhone = async (phoneId: string) => {
  const adminId = getAdminId();
  if (!adminId) {
    throw new Error('Admin session missing. Please log in again.');
  }

  const response = await client.delete(`/admin/phones/${phoneId}`, {
    params: { adminId }
  });

  if (!response.data?.success) {
    throw new Error(response.data?.message || 'Failed to delete phone');
  }

  return response.data;
};

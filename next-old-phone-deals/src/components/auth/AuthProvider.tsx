'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { apiGet } from '@/lib/apiClient';
import {
  getUserToken,
  getAdminToken,
  setUserToken,
  setAdminToken,
  clearUserToken,
  clearAdminToken,
  clearAllTokens
} from '@/lib/authStorage';
import type { User, Admin, ApiResponse } from '@/types/auth';

type AuthContextType = {
  user: User | null;
  admin: Admin | null;
  isLoading: boolean;
  setUser: (user: User | null) => void;
  setAdmin: (admin: Admin | null) => void;
  loginUser: (token: string, userData: User) => void;
  loginAdmin: (token: string, adminData: Admin) => void;
  logout: () => void;
  refetchUser: () => Promise<void>;
  refetchAdmin: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

type AuthProviderProps = {
  children: React.ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // 获取用户信息
  const refetchUser = useCallback(async () => {
    const token = getUserToken();
    if (!token) {
      setUser(null);
      return;
    }

    try {
      const response = await apiGet<ApiResponse<User>>('/auth/me', {
        authToken: token
      });

      if (response.success && response.data) {
        setUser(response.data);
      } else {
        clearUserToken();
        setUser(null);
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error);
      clearUserToken();
      setUser(null);
    }
  }, []);

  // 获取管理员信息
  const refetchAdmin = useCallback(async () => {
    const token = getAdminToken();
    if (!token) {
      setAdmin(null);
      return;
    }

    try {
      const response = await apiGet<ApiResponse<Admin>>('/admin/profile', {
        authToken: token
      });

      if (response.success && response.data) {
        setAdmin(response.data);
      } else {
        clearAdminToken();
        setAdmin(null);
      }
    } catch (error) {
      console.error('Failed to fetch admin info:', error);
      clearAdminToken();
      setAdmin(null);
    }
  }, []);

  // 初始化：从 localStorage 加载 token 并获取用户信息
  useEffect(() => {
    const initAuth = async () => {
      setIsLoading(true);

      // 优先检查管理员 token
      const adminToken = getAdminToken();
      if (adminToken) {
        await refetchAdmin();
      }

      // 然后检查用户 token
      const userToken = getUserToken();
      if (userToken) {
        await refetchUser();
      }

      setIsLoading(false);
    };

    initAuth();
  }, [refetchUser, refetchAdmin]);

  // 用户登录成功
  const loginUser = useCallback((token: string, userData: User) => {
    setUserToken(token);
    setUser(userData);
    // 清除管理员状态
    clearAdminToken();
    setAdmin(null);
  }, []);

  // 管理员登录成功
  const loginAdmin = useCallback((token: string, adminData: Admin) => {
    setAdminToken(token);
    setAdmin(adminData);
    // 清除用户状态
    clearUserToken();
    setUser(null);
  }, []);

  // 登出
  const logout = useCallback(() => {
    clearAllTokens();
    setUser(null);
    setAdmin(null);
    router.push('/');
  }, [router]);

  const value: AuthContextType = {
    user,
    admin,
    isLoading,
    setUser,
    setAdmin,
    loginUser,
    loginAdmin,
    logout,
    refetchUser,
    refetchAdmin
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
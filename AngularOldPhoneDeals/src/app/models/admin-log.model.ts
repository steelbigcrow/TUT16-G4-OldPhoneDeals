export interface AdminLog {
  id: string;
  adminUserId: string;
  action: string;
  targetType: string;
  targetId: string;
  createdAt: string;
}

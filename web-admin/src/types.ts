export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface User {
  id: number;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
  status: UserStatus;
  residence: string;
  address: string;
  createdAt: string;
}

export interface Residence {
  id: number;
  name: string;
  type: string;
  address: string;
  buildingNo?: string;
  area?: string;
  approval: ReviewStatus;
}

export interface CollectionArea {
  id: number;
  name: string;
  district: string;
  dongs: string[];
  active: boolean;
  updatedAt: string;
}

export interface Schedule {
  id: number;
  area: string;
  category: string;
  day: string;
  time: string;
  note: string;
  active: boolean;
}

export interface WasteItem {
  id: number;
  name: string;
  category: string;
  aliases: string[];
  aiSupported: boolean;
  active: boolean;
}

export interface Guide {
  id: number;
  wasteItem: string;
  summary: string;
  method: string;
  caution: string;
  checks: string[];
}

export interface AiCorrection {
  id: number;
  imageLogId: number;
  userEmail: string;
  aiItem: string;
  aiConfidence: number;
  correctedItem: string;
  status: ReviewStatus;
  correctedAt: string;
  memo?: string;
}

export interface SyncLog {
  id: number;
  source: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  inserted: number;
  updated: number;
  failed: number;
  at: string;
}

export interface Notification {
  id: number;
  title: string;
  body: string;
  target: string;
  status: 'DRAFT' | 'SCHEDULED' | 'SENT';
  at?: string;
}

export interface Dashboard {
  users: number;
  activeUsers: number;
  pendingResidences: number;
  wasteItems: number;
  pendingAi: number;
  scheduledNotifications: number;
}

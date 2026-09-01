export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';
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

export type CollectionAreaSourceType =
  | 'MOIS_HOUSEHOLD_WASTE'
  | 'MANUAL';

export type CollectionWasteType =
  | 'LIFE_WASTE'
  | 'FOOD_WASTE'
  | 'RECYCLABLE';

export interface CollectionArea {
  id: number;
  name: string;
  sido?: string;
  district: string;
  dongs: string[];
  targetAreaName?: string;
  externalManagementNumber?: string;
  wasteTypes?: CollectionWasteType[];
  active: boolean;
  updatedAt: string;
  sourceType?: CollectionAreaSourceType;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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


export type CollectionScheduleSourceType =
  | 'PUBLIC_DATA'
  | 'ADMIN_APPROVED_REPORT';

export interface GeneralHousingSchedule {
  id: number;
  collectionAreaId: number;
  collectionAreaName: string;
  wasteType: CollectionWasteType;
  sourceType: CollectionScheduleSourceType;
  day: string;
  startTime?: string;
  endTime?: string;
  time: string;
  note: string;
}

export interface AreaScheduleCoverage {
  collectionAreaId: number;
  collectionAreaName: string;
  areaSourceType: CollectionAreaSourceType;
  externalManagementNumber?: string;
  sido: string;
  district: string;
  targetAreaName?: string;
  active: boolean;
  supportedWasteTypes: CollectionWasteType[];
  schedules: GeneralHousingSchedule[];
  missingWasteTypes: CollectionWasteType[];
}


export interface WasteTypeCoverage {
  wasteType: CollectionWasteType;
  supportedAreaCount: number;
  registeredAreaCount: number;
  missingAreaCount: number;
}

export interface AreaScheduleGroupCoverage {
  representativeCollectionAreaId: number;
  collectionAreaName: string;
  sido: string;
  district: string;
  targetAreaName?: string;
  areaSourceType: CollectionAreaSourceType;
  active: boolean;
  collectionAreaCount: number;
  allSchedulesRegistered: boolean;
  supportedWasteTypes: CollectionWasteType[];
  wasteTypeCoverage: WasteTypeCoverage[];
  areas: AreaScheduleCoverage[];
}

export interface ApartmentSchedule {
  id: number;
  apartmentId: number;
  apartmentName: string;
  wasteItemId: number;
  wasteItemName: string;
  dayOfWeek: string;
  startTime?: string;
  endTime?: string;
  alwaysAvailable: boolean;
  createdAt?: string;
  updatedAt?: string;
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
  status: 'SENT' | 'CANCELLED';
  sentAt?: string;
}

export interface Dashboard {
  users: number;
  activeUsers: number;
  pendingResidences: number;
  wasteItems: number;
  pendingAi: number;
  todayNotifications: number;
}

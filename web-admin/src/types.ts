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

/**
 * 실제 CollectionArea 원본.
 *
 * 관리자 그룹 상세에서 각 공공데이터/수동 등록 원본을
 * 확인하거나 수정할 때 사용합니다.
 */
export interface CollectionArea {
  id: number;
  name: string;
  sido?: string;
  district: string;
  dongs: string[];
  targetAreaName?: string;
  externalManagementNumber?: string;
  wasteTypes?: CollectionWasteType[];
  sourceReferenceDate?: string;
  active: boolean;
  createdAt?: string;
  updatedAt: string;
  sourceType?: CollectionAreaSourceType;
}

/**
 * 관리자 수거구역 목록의 한 행.
 *
 * 실제 CollectionArea 원본 여러 건을
 * 화면에서 하나의 지역 그룹으로 표시합니다.
 */
export interface CollectionAreaGroup {
  sido: string;
  district: string;
  targetAreaName: string;
  sourceType: CollectionAreaSourceType;
  active: boolean;
  originalCount: number;
}

/**
 * 지역 그룹 상세.
 *
 * 목록에서 묶어 놓은 실제 CollectionArea 원본을
 * originals에서 모두 확인할 수 있습니다.
 */
export interface CollectionAreaGroupDetail extends CollectionAreaGroup {
  originals: CollectionArea[];
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
  imageUrl?: string;
  userDescription?: string;
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

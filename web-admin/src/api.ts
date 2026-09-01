import { CONFIG, ENDPOINTS } from './config';
import {
  initialAreas,
  initialCorrections,
  initialDashboard,
  initialGuides,
  initialNotifications,
  initialResidences,
  initialSchedules,
  initialSyncLogs,
  initialUsers,
  initialWasteItems,
} from './mockData';
import type {
  AiCorrection,
  ApartmentSchedule,
  AreaScheduleCoverage,
  CollectionArea,
  CollectionWasteType,
  Dashboard,
  GeneralHousingSchedule,
  Guide,
  Notification,
  PageResult,
  Residence,
  ReviewStatus,
  Schedule,
  SyncLog,
  User,
  UserStatus,
  WasteItem,
} from './types';

const ACCESS = 'smartrecycle_admin_access';
const REFRESH = 'smartrecycle_admin_refresh';
const SESSION = 'smartrecycle_admin_session';

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
type JsonRecord = Record<string, unknown>;

type BackendTokenResponse = {
  accessToken: string;
  refreshToken: string;
};

type BackendDashboard = {
  totalUsers: number;
  activeUsers: number;
  pendingResidences: number;
  wasteItems: number;
  pendingAiCorrections: number;
  todayNotifications: number;
};

type BackendUser = {
  id: number;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';
  residenceType?: 'MANAGED_COMPLEX' | 'GENERAL_HOUSING' | null;
  residenceName?: string | null;
  address?: string | null;
  createdAt?: string | null;
};

type BackendApartment = {
  id: number;
  name: string;
  roadAddress?: string | null;
  jibunAddress?: string | null;
  buildingManagementNumber?: string | null;
  status: ReviewStatus;
  createAt?: string | null;
  updateAt?: string | null;
};

type BackendCollectionArea = {
  id: number;
  sourceType: 'MOIS_HOUSEHOLD_WASTE' | 'MANUAL';
  externalManagementNumber?: string | null;
  sido: string;
  sigungu: string;
  areaName: string;
  targetAreaName?: string | null;
  supportedWasteTypes: Array<'LIFE_WASTE' | 'FOOD_WASTE' | 'RECYCLABLE'>;
  active: boolean;
  updatedAt?: string | null;
};

type BackendCollectionAreaSchedule = {
  id: number;
  collectionAreaId: number;
  collectionAreaName: string;
  wasteType: 'LIFE_WASTE' | 'FOOD_WASTE' | 'RECYCLABLE';
  sourceType: 'PUBLIC_DATA' | 'ADMIN_APPROVED_REPORT';
  emissionDays: string;
  startTime?: string | null;
  endTime?: string | null;
  emissionMethod?: string | null;
  emissionPlace?: string | null;
  emissionPlaceType?: string | null;
  uncollectedDay?: string | null;
};


type BackendAreaScheduleCoverage = {
  collectionAreaId: number;
  collectionAreaName: string;
  areaSourceType: 'MOIS_HOUSEHOLD_WASTE' | 'MANUAL';
  externalManagementNumber?: string | null;
  sido: string;
  sigungu: string;
  targetAreaName?: string | null;
  active: boolean;
  supportedWasteTypes: Array<
    'LIFE_WASTE' | 'FOOD_WASTE' | 'RECYCLABLE'
  >;
  schedules: BackendCollectionAreaSchedule[];
  missingWasteTypes: Array<
    'LIFE_WASTE' | 'FOOD_WASTE' | 'RECYCLABLE'
  >;
};

type BackendApartmentSchedule = {
  id: number;
  apartmentId: number;
  apartmentName: string;
  wasteItem: {
    id: number;
    name: string;
  };
  dayOfWeek: string;
  startTime?: string | null;
  endTime?: string | null;
  alwaysAvailable: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendWasteCategory = {
  id: number;
  code: string;
  name: string;
  active: boolean;
};

type BackendWasteItem = {
  id: number;
  name: string;
  searchKeywords?: string | null;
  imageUrl?: string | null;
  category: {
    id: number;
    code: string;
    name: string;
  };
  active: boolean;
};

type BackendWasteDetail = {
  id: number;
  name: string;
  guide?: {
    id: number;
    summary: string;
    disposalMethod: string;
    caution?: string | null;
    checkItems: Array<{
      id: number;
      content: string;
      sortOrder: number;
      required: boolean;
    }>;
  } | null;
};

type BackendCorrection = {
  id: number;
  imageLogId: number;
  userEmail: string;
  aiWasteItemName?: string | null;
  aiConfidence?: number | null;
  correctedWasteItemName?: string | null;
  reviewStatus: ReviewStatus;
  correctedAt?: string | null;
  reviewMemo?: string | null;
};

type BackendSyncLog = {
  id: number;
  source: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  startedAt?: string | null;
  finishedAt?: string | null;
  insertedCount: number;
  updatedCount: number;
  failedCount: number;
};

type BackendNotification = {
  id: number;
  title: string;
  body: string;
  targetType: string;
  status: 'SENT' | 'CANCELLED';
  sentAt?: string | null;
};

class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

function clearAuth() {
  localStorage.removeItem(ACCESS);
  localStorage.removeItem(REFRESH);
  localStorage.removeItem(SESSION);
}

function expireSession() {
  clearAuth();
  window.dispatchEvent(new Event('smartrecycle-auth-expired'));
}

async function readPayload(response: Response): Promise<unknown> {
  const raw = await response.text();
  if (!raw) return null;

  try {
    return JSON.parse(raw) as unknown;
  } catch {
    return raw;
  }
}

function payloadMessage(payload: unknown, fallback: string): string {
  if (payload && typeof payload === 'object') {
    const value = payload as JsonRecord;
    if (typeof value.message === 'string' && value.message.trim()) {
      return value.message;
    }
  }

  return fallback;
}

function unwrapData<T>(payload: unknown): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as { data: T }).data;
  }

  return payload as T;
}

async function requestWithoutAuth<T>(
  path: string,
  method: Method = 'GET',
  body?: unknown,
): Promise<T> {
  const response = await fetch(`${CONFIG.apiBaseUrl}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const payload = await readPayload(response);

  if (!response.ok) {
    throw new HttpError(
      response.status,
      payloadMessage(payload, `요청 실패 (${response.status})`),
    );
  }

  return unwrapData<T>(payload);
}

let refreshPromise: Promise<boolean> | null = null;

async function reissueTokens(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH);
  if (!refreshToken) return false;

  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const tokens = await requestWithoutAuth<BackendTokenResponse>(
          ENDPOINTS.reissue,
          'POST',
          { refreshToken },
        );

        if (!tokens?.accessToken || !tokens?.refreshToken) {
          return false;
        }

        localStorage.setItem(ACCESS, tokens.accessToken);
        localStorage.setItem(REFRESH, tokens.refreshToken);
        return true;
      } catch {
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
  }

  return refreshPromise;
}

async function request<T>(
  path: string,
  method: Method = 'GET',
  body?: unknown,
  retryOnUnauthorized = true,
): Promise<T> {
  const token = localStorage.getItem(ACCESS);

  const response = await fetch(`${CONFIG.apiBaseUrl}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && retryOnUnauthorized) {
    const refreshed = await reissueTokens();
    if (refreshed) {
      return request<T>(path, method, body, false);
    }

    expireSession();
    throw new HttpError(401, '로그인이 만료되었습니다.');
  }

  const payload = await readPayload(response);

  if (!response.ok) {
    if (response.status === 401) {
      expireSession();
    }

    throw new HttpError(
      response.status,
      payloadMessage(payload, `요청 실패 (${response.status})`),
    );
  }

  return unwrapData<T>(payload);
}

function pageItems<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];

  if (value && typeof value === 'object') {
    const record = value as JsonRecord;
    if (Array.isArray(record.content)) return record.content as T[];
    if (Array.isArray(record.items)) return record.items as T[];
  }

  return [];
}

function pageResult<T>(
  value: unknown,
  fallbackPage = 0,
  fallbackSize = 20,
): PageResult<T> {
  if (Array.isArray(value)) {
    const items = value as T[];
    return {
      items,
      page: fallbackPage,
      size: fallbackSize,
      totalElements: items.length,
      totalPages: items.length === 0 ? 0 : 1,
    };
  }

  if (!value || typeof value !== 'object') {
    return {
      items: [],
      page: fallbackPage,
      size: fallbackSize,
      totalElements: 0,
      totalPages: 0,
    };
  }

  const record = value as JsonRecord;
  const items = pageItems<T>(record);

  const numberValue = (...keys: string[]): number | undefined => {
    for (const key of keys) {
      const candidate = record[key];
      if (typeof candidate === 'number' && Number.isFinite(candidate)) {
        return candidate;
      }
    }
    return undefined;
  };

  const page =
    numberValue('page', 'pageNumber', 'number', 'currentPage') ??
    fallbackPage;

  const size =
    numberValue('size', 'pageSize', 'limit') ??
    fallbackSize;

  const totalElements =
    numberValue('totalElements', 'totalCount', 'total') ??
    items.length;

  const totalPages =
    numberValue('totalPages', 'pageCount') ??
    (totalElements === 0
      ? 0
      : Math.ceil(totalElements / Math.max(size, 1)));

  return {
    items,
    page,
    size,
    totalElements,
    totalPages,
  };
}

function delay() {
  return new Promise<void>((resolve) => setTimeout(resolve, 150));
}

function formatDate(value?: string | null): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function splitTargetAreas(value?: string | null): string[] {
  if (!value) return [];

  return value
    .split(/[,/·]|\s{2,}/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function backendUserStatusToUi(
  status: BackendUser['status'],
): UserStatus {
  return status;
}

function uiUserStatusToBackend(
  status: UserStatus,
): BackendUser['status'] {
  return status;
}

function wasteTypeLabel(
  wasteType: BackendCollectionAreaSchedule['wasteType'],
): string {
  if (wasteType === 'LIFE_WASTE') return '일반쓰레기';
  if (wasteType === 'FOOD_WASTE') return '음식물류';
  return '재활용품';
}

function uiCategoryToWasteType(
  category: string,
): BackendCollectionAreaSchedule['wasteType'] {
  const normalized = category.replace(/\s/g, '');

  if (normalized.includes('음식')) return 'FOOD_WASTE';
  if (
    normalized.includes('일반') ||
    normalized.includes('종량제') ||
    normalized.includes('생활쓰레기')
  ) {
    return 'LIFE_WASTE';
  }

  return 'RECYCLABLE';
}

function scheduleTime(start?: string | null, end?: string | null): string {
  if (!start && !end) return '시간 정보 없음';
  if (start && end) return `${start.slice(0, 5)} ~ ${end.slice(0, 5)}`;
  return start?.slice(0, 5) ?? end?.slice(0, 5) ?? '시간 정보 없음';
}


function mapGeneralSchedule(
  item: BackendCollectionAreaSchedule,
): GeneralHousingSchedule {
  return {
    id: item.id,
    collectionAreaId: item.collectionAreaId,
    collectionAreaName: item.collectionAreaName,
    wasteType: item.wasteType,
    sourceType: item.sourceType,
    day: item.emissionDays,
    startTime:
      item.startTime?.slice(0, 5) ?? undefined,
    endTime:
      item.endTime?.slice(0, 5) ?? undefined,
    time: scheduleTime(
      item.startTime,
      item.endTime,
    ),
    note: [
      item.emissionMethod,
      item.emissionPlace,
      item.uncollectedDay,
    ]
      .filter(Boolean)
      .join(' · '),
  };
}


function mapAreaScheduleCoverage(
  item: BackendAreaScheduleCoverage,
): AreaScheduleCoverage {
  return {
    collectionAreaId:
      item.collectionAreaId,
    collectionAreaName:
      item.collectionAreaName,
    areaSourceType:
      item.areaSourceType,
    externalManagementNumber:
      item.externalManagementNumber ??
      undefined,
    sido: item.sido,
    district:
      item.sigungu,
    targetAreaName:
      item.targetAreaName ??
      undefined,
    active: item.active,
    supportedWasteTypes:
      item.supportedWasteTypes,
    schedules:
      item.schedules.map(
        mapGeneralSchedule,
      ),
    missingWasteTypes:
      item.missingWasteTypes,
  };
}

function parseTimeRange(value: string): {
  startTime: string | null;
  endTime: string | null;
} {
  const matches = value.match(/(\d{1,2}):(\d{2})/g);
  if (!matches || matches.length === 0) {
    return { startTime: null, endTime: null };
  }

  if (matches.length !== 2) {
    throw new Error('시간은 예: 18:00 ~ 21:00 형식으로 입력해주세요.');
  }

  return {
    startTime: matches[0],
    endTime: matches[1],
  };
}

function notificationTargetLabel(targetType: string): string {
  const labels: Record<string, string> = {
    ALL_ACTIVE_USERS: '전체 사용자',
    NOTIFICATION_ENABLED_USERS: '알림 수신 동의 사용자',
    MANAGED_COMPLEX_USERS: '공동주택 사용자',
    GENERAL_HOUSING_USERS: '일반주택 사용자',
  };

  return labels[targetType] ?? targetType;
}

function notificationTargetValue(target: string): string {
  const normalized = target.replace(/\s/g, '');

  if (normalized === '전체사용자') return 'ALL_ACTIVE_USERS';
  if (normalized.includes('알림수신')) return 'NOTIFICATION_ENABLED_USERS';
  if (normalized.includes('공동주택') || normalized.includes('아파트')) {
    return 'MANAGED_COMPLEX_USERS';
  }
  if (normalized.includes('일반주택')) return 'GENERAL_HOUSING_USERS';

  throw new Error(
    '알림 대상은 전체 사용자, 알림 수신 동의 사용자, 공동주택 사용자, 일반주택 사용자 중 하나를 입력해주세요.',
  );
}

const AI_SUPPORTED_ITEM_NAMES = new Set([
  '종이박스',
  '페트병',
  '플라스틱 용기',
  '캔',
  '유리병',
  '스티로폼',
]);

let users = [...initialUsers];
let residences = [...initialResidences];
let areas = [...initialAreas];
let schedules = [...initialSchedules];
let wasteItems = [...initialWasteItems];
let guides = [...initialGuides];
let corrections = [...initialCorrections];
let syncLogs = [...initialSyncLogs];
let notifications = [...initialNotifications];

const areaMeta = new Map<number, BackendCollectionArea>();
const scheduleMeta = new Map<number, BackendCollectionAreaSchedule>();
const wasteItemMeta = new Map<number, BackendWasteItem>();

async function backendWasteCategories(): Promise<BackendWasteCategory[]> {
  const data = await request<unknown>(ENDPOINTS.wasteCategories);
  return pageItems<BackendWasteCategory>(data);
}

async function findWasteCategory(name: string): Promise<BackendWasteCategory> {
  const categories = await backendWasteCategories();
  const normalized = name.trim().toLowerCase();

  const exact = categories.find(
    (category) => category.name.trim().toLowerCase() === normalized,
  );
  if (exact) return exact;

  const fuzzy = categories.find((category) => {
    const categoryName = category.name.trim().toLowerCase();
    return categoryName.includes(normalized) || normalized.includes(categoryName);
  });
  if (fuzzy) return fuzzy;

  throw new Error(
    `카테고리 '${name}'을 찾지 못했습니다. 등록된 카테고리명을 사용해주세요.`,
  );
}

async function findCollectionAreaByName(
  name: string,
): Promise<BackendCollectionArea> {
  const data = await request<unknown>(
    `${ENDPOINTS.collectionAreas}?keyword=${encodeURIComponent(name)}&size=50`,
  );
  const candidates = pageItems<BackendCollectionArea>(data);
  const normalized = name.trim().toLowerCase();

  const exact = candidates.find(
    (area) => area.areaName.trim().toLowerCase() === normalized,
  );

  if (exact) return exact;
  if (candidates.length === 1) return candidates[0];

  throw new Error(`수거구역 '${name}'을 정확히 찾지 못했습니다.`);
}

export const authApi = {
  session() {
    const raw = localStorage.getItem(SESSION);
    return raw ? (JSON.parse(raw) as { email: string; role: string }) : null;
  },

  async login(email: string, password: string) {
    if (CONFIG.useMocks) {
      await delay();
      if (email !== 'admin@smartrecycle.com' || password !== 'Admin1234!') {
        throw new Error('관리자 이메일 또는 비밀번호를 확인해주세요.');
      }

      const session = { email, role: 'ADMIN' };
      localStorage.setItem(ACCESS, 'mock-access-token');
      localStorage.setItem(REFRESH, 'mock-refresh-token');
      localStorage.setItem(SESSION, JSON.stringify(session));
      return session;
    }

    const tokens = await requestWithoutAuth<BackendTokenResponse>(
      ENDPOINTS.login,
      'POST',
      { email, password },
    );

    if (!tokens?.accessToken || !tokens?.refreshToken) {
      throw new Error('로그인 토큰을 확인하지 못했습니다.');
    }

    localStorage.setItem(ACCESS, tokens.accessToken);
    localStorage.setItem(REFRESH, tokens.refreshToken);

    try {
      // 로그인 응답에는 role이 없으므로 관리자 API를 1회 호출해
      // 실제 ADMIN 권한을 서버에서 검증합니다.
      await request<BackendDashboard>(ENDPOINTS.dashboard);
    } catch (error) {
      clearAuth();

      if (error instanceof HttpError && error.status === 403) {
        throw new Error('관리자 권한이 있는 계정만 로그인할 수 있습니다.');
      }

      throw error;
    }

    const session = { email, role: 'ADMIN' };
    localStorage.setItem(SESSION, JSON.stringify(session));
    return session;
  },

  logout() {
    clearAuth();
  },
};

export const adminApi = {
  async dashboard(): Promise<Dashboard> {
    if (CONFIG.useMocks) {
      await delay();
      return {
        ...initialDashboard,
        users: users.length,
        activeUsers: users.filter((item) => item.status === 'ACTIVE').length,
        pendingResidences: residences.filter((item) => item.approval === 'PENDING').length,
        wasteItems: wasteItems.length,
        pendingAi: corrections.filter((item) => item.status === 'PENDING').length,
        todayNotifications: notifications.filter((item) => item.status === 'SENT').length,
      };
    }

    const data = await request<BackendDashboard>(ENDPOINTS.dashboard);

    return {
      users: data.totalUsers,
      activeUsers: data.activeUsers,
      pendingResidences: data.pendingResidences,
      wasteItems: data.wasteItems,
      pendingAi: data.pendingAiCorrections,
      todayNotifications: data.todayNotifications,
    };
  },

  async users(options?: {
    keyword?: string;
    page?: number;
    size?: number;
  }): Promise<PageResult<User>> {
    const keyword = options?.keyword?.trim() ?? '';
    const page = Math.max(options?.page ?? 0, 0);
    const size = Math.max(options?.size ?? 20, 1);

    const mapUser = (item: BackendUser): User => ({
      id: item.id,
      email: item.email,
      name: item.name,
      role: item.role,
      status: backendUserStatusToUi(item.status),
      residence: item.residenceName ?? '미설정',
      address: item.address ?? '-',
      createdAt: formatDate(item.createdAt),
    });

    if (CONFIG.useMocks) {
      await delay();

      const normalized = keyword.toLowerCase();
      const filtered = users.filter(
        (item) =>
          !normalized ||
          [
            item.email,
            item.name,
            item.residence,
            item.address,
          ].some((value) =>
            value.toLowerCase().includes(normalized),
          ),
      );

      const start = page * size;
      const items = filtered.slice(start, start + size);

      return {
        items,
        page,
        size,
        totalElements: filtered.length,
        totalPages:
          filtered.length === 0
            ? 0
            : Math.ceil(filtered.length / size),
      };
    }

    const params = new URLSearchParams({
      keyword,
      page: String(page),
      size: String(size),
      sort: 'createdAt,desc',
    });

    const data = await request<unknown>(
      `${ENDPOINTS.users}?${params.toString()}`,
    );

    const backendPage = pageResult<BackendUser>(
      data,
      page,
      size,
    );

    return {
      ...backendPage,
      items: backendPage.items.map(mapUser),
    };
  },

  async setUserStatus(id: number, status: UserStatus) {
    if (CONFIG.useMocks) {
      await delay();
      users = users.map((item) => (item.id === id ? { ...item, status } : item));
      return;
    }

    await request(ENDPOINTS.userStatus(id), 'PATCH', {
      status: uiUserStatusToBackend(status),
    });
  },

  async setUserRole(id: number, role: 'USER' | 'ADMIN') {
    if (CONFIG.useMocks) {
      await delay();
      users = users.map((item) => (item.id === id ? { ...item, role } : item));
      return;
    }

    await request(ENDPOINTS.userRole(id), 'PATCH', { role });
  },

  async residences(): Promise<Residence[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [...residences];
    }

    const statuses: ReviewStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];

    const pages = await Promise.all(
      statuses.map((status) =>
        request<unknown>(`${ENDPOINTS.apartments}?status=${status}&size=200`),
      ),
    );

    return pages
      .flatMap((page) => pageItems<BackendApartment>(page))
      .map((item) => ({
        id: item.id,
        name: item.name,
        type: '공동주택',
        address: item.roadAddress || item.jibunAddress || '-',
        buildingNo: item.buildingManagementNumber ?? undefined,
        area: undefined,
        approval: item.status,
      }));
  },

  async reviewResidence(id: number, approved: boolean) {
    if (CONFIG.useMocks) {
      await delay();
      residences = residences.map((item) =>
        item.id === id
          ? { ...item, approval: approved ? 'APPROVED' : 'REJECTED' }
          : item,
      );
      return;
    }

    if (approved) {
      await request(ENDPOINTS.approveApartment(id), 'PATCH');
      return;
    }

    await request(ENDPOINTS.rejectApartment(id), 'PATCH', {
      rejectionReason: '관리자 검수 결과 등록 요청을 거절했습니다.',
    });
  },

  async areas(options?: {
    keyword?: string;
    sourceType?: 'MOIS_HOUSEHOLD_WASTE' | 'MANUAL' | '';
    active?: '' | 'true' | 'false';
    page?: number;
    size?: number;
  }): Promise<PageResult<CollectionArea>> {
    const keyword = options?.keyword?.trim() ?? '';
    const sourceType = options?.sourceType ?? '';
    const active = options?.active ?? '';
    const page = Math.max(options?.page ?? 0, 0);
    const size = Math.max(options?.size ?? 20, 1);

    if (CONFIG.useMocks) {
      await delay();

      let filtered = [...areas];

      if (keyword) {
        const normalized = keyword.toLowerCase();
        filtered = filtered.filter((item) =>
          [
            item.name,
            item.sido ?? '',
            item.district,
            item.dongs.join(' '),
            item.externalManagementNumber ?? '',
          ]
            .join(' ')
            .toLowerCase()
            .includes(normalized),
        );
      }

      if (sourceType) {
        filtered = filtered.filter(
          (item) => item.sourceType === sourceType,
        );
      }

      if (active) {
        filtered = filtered.filter(
          (item) => item.active === (active === 'true'),
        );
      }

      const start = page * size;
      const items = filtered.slice(start, start + size);

      return {
        items,
        page,
        size,
        totalElements: filtered.length,
        totalPages:
          filtered.length === 0
            ? 0
            : Math.ceil(filtered.length / size),
      };
    }

    const params = new URLSearchParams({
      keyword,
      page: String(page),
      size: String(size),
      sort: 'updatedAt,desc',
    });

    if (sourceType) {
      params.set('sourceType', sourceType);
    }

    if (active) {
      params.set('active', active);
    }

    const data = await request<unknown>(
      `${ENDPOINTS.collectionAreas}?${params.toString()}`,
    );

    const backendPage = pageResult<BackendCollectionArea>(
      data,
      page,
      size,
    );

    areaMeta.clear();
    backendPage.items.forEach((item) => areaMeta.set(item.id, item));

    return {
      ...backendPage,
      items: backendPage.items.map((item) => ({
        id: item.id,
        name: item.areaName,
        sido: item.sido,
        district: item.sigungu,
        dongs: splitTargetAreas(item.targetAreaName),
        targetAreaName: item.targetAreaName ?? '',
        externalManagementNumber:
          item.externalManagementNumber ?? '',
        wasteTypes: item.supportedWasteTypes,
        active: item.active,
        updatedAt: formatDate(item.updatedAt),
        sourceType: item.sourceType,
      })),
    };
  },

  async saveArea(item: Partial<CollectionArea> & { name: string; district: string }) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) {
        areas = areas.map((area) =>
          area.id === item.id
            ? { ...area, ...item, dongs: item.dongs ?? area.dongs } as CollectionArea
            : area,
        );
      } else {
        areas = [
          ...areas,
          {
            id: Date.now(),
            name: item.name,
            district: item.district,
            dongs: item.dongs ?? [],
            active: item.active ?? true,
            updatedAt: new Date().toLocaleString('ko-KR'),
          },
        ];
      }
      return;
    }

    const targetAreaName = (item.dongs ?? []).join(', ');

    if (item.id) {
      const existing = areaMeta.get(item.id);
      if (!existing) {
        throw new Error('수정할 수거구역 원본 정보를 다시 불러와주세요.');
      }
      if (existing.sourceType !== 'MANUAL') {
        throw new Error('공공데이터 수거구역은 직접 수정할 수 없습니다.');
      }

      await request(ENDPOINTS.collectionArea(item.id), 'PUT', {
        sido: existing.sido,
        sigungu: item.district,
        areaName: item.name,
        targetAreaName: targetAreaName || null,
        supportedWasteTypes: existing.supportedWasteTypes,
        active: item.active ?? existing.active,
      });
      return;
    }

    await request(ENDPOINTS.collectionAreas, 'POST', {
      sido: '부산광역시',
      sigungu: item.district,
      areaName: item.name,
      targetAreaName: targetAreaName || null,
      supportedWasteTypes: ['LIFE_WASTE', 'FOOD_WASTE', 'RECYCLABLE'],
    });
  },

  async setAreaActive(id: number, active: boolean) {
    if (CONFIG.useMocks) {
      await delay();
      areas = areas.map((area) =>
        area.id === id ? { ...area, active } : area,
      );
      return;
    }

    await request(
      active
        ? ENDPOINTS.collectionAreaActivate(id)
        : ENDPOINTS.collectionAreaDeactivate(id),
      'PATCH',
    );
  },

  async generalScheduleCoverage(options?: {
    keyword?: string;
    sourceType?:
      | ''
      | 'MOIS_HOUSEHOLD_WASTE'
      | 'MANUAL';
    active?: '' | 'true' | 'false';
    page?: number;
    size?: number;
  }): Promise<PageResult<AreaScheduleGroupCoverage>> {
    const keyword =
      options?.keyword?.trim() ?? '';
    const sourceType =
      options?.sourceType ?? '';
    const active =
      options?.active ?? '';
    const page =
      Math.max(options?.page ?? 0, 0);
    const size =
      Math.max(options?.size ?? 20, 1);

    if (CONFIG.useMocks) {
      await delay();

      const areaPage =
        await adminApi.areas({
          keyword,
          sourceType,
          active,
          page,
          size,
        });

      return {
        ...areaPage,
        items: areaPage.items.map(
          (area) => {
            const areaSchedules =
              schedules
                .filter(
                  (schedule) =>
                    schedule.area ===
                    area.name,
                )
                .map<GeneralHousingSchedule>(
                  (schedule) => ({
                    id: schedule.id,
                    collectionAreaId:
                      area.id,
                    collectionAreaName:
                      area.name,
                    wasteType:
                      uiCategoryToWasteType(
                        schedule.category,
                      ),
                    sourceType:
                      'ADMIN_APPROVED_REPORT',
                    day: schedule.day,
                    time: schedule.time,
                    note: schedule.note,
                  }),
                );

            const registered =
              new Set(
                areaSchedules.map(
                  (schedule) =>
                    schedule.wasteType,
                ),
              );

            const supportedWasteTypes =
              area.wasteTypes ?? [];

            const areaCoverage: AreaScheduleCoverage = {
              collectionAreaId:
                area.id,
              collectionAreaName:
                area.name,
              areaSourceType:
                area.sourceType ??
                'MOIS_HOUSEHOLD_WASTE',
              externalManagementNumber:
                area.externalManagementNumber,
              sido: area.sido ?? '',
              district:
                area.district,
              targetAreaName:
                area.targetAreaName ??
                area.dongs.join(', '),
              active: area.active,
              supportedWasteTypes,
              schedules:
                areaSchedules,
              missingWasteTypes:
                supportedWasteTypes.filter(
                  (wasteType) =>
                    !registered.has(
                      wasteType,
                    ),
                ),
            };

            return {
              representativeCollectionAreaId:
                area.id,
              collectionAreaName:
                area.name,
              sido:
                area.sido ?? '',
              district:
                area.district,
              targetAreaName:
                area.targetAreaName ??
                area.dongs.join(', '),
              areaSourceType:
                area.sourceType ??
                'MOIS_HOUSEHOLD_WASTE',
              active:
                area.active,
              collectionAreaCount:
                1,
              allSchedulesRegistered:
                areaCoverage
                  .missingWasteTypes
                  .length === 0,
              supportedWasteTypes,
              wasteTypeCoverage:
                supportedWasteTypes.map(
                  (wasteType) => {
                    const isRegistered =
                      registered.has(
                        wasteType,
                      );

                    return {
                      wasteType,
                      supportedAreaCount:
                        1,
                      registeredAreaCount:
                        isRegistered
                          ? 1
                          : 0,
                      missingAreaCount:
                        isRegistered
                          ? 0
                          : 1,
                    };
                  },
                ),
              areas: [
                areaCoverage,
              ],
            };
          },
        ),
      };
    }

    const params =
      new URLSearchParams({
        keyword,
        page: String(page),
        size: String(size),
      });

    if (sourceType) {
      params.set(
        'sourceType',
        sourceType,
      );
    }

    if (active) {
      params.set(
        'active',
        active,
      );
    }

    const data =
      await request<unknown>(
        `${ENDPOINTS.collectionAreaSchedules}/coverage?${params.toString()}`,
      );

    const backendPage =
      pageResult<BackendAreaScheduleGroupCoverage>(
        data,
        page,
        size,
      );

    return {
      ...backendPage,
      items:
        backendPage.items.map(
          (item) => ({
            representativeCollectionAreaId:
              item.representativeCollectionAreaId,
            collectionAreaName:
              item.collectionAreaName,
            sido:
              item.sido,
            district:
              item.sigungu,
            targetAreaName:
              item.targetAreaName ??
              undefined,
            areaSourceType:
              item.areaSourceType,
            active:
              item.active,
            collectionAreaCount:
              item.collectionAreaCount,
            allSchedulesRegistered:
              item.allSchedulesRegistered,
            supportedWasteTypes:
              item.supportedWasteTypes,
            wasteTypeCoverage:
              item.wasteTypeCoverage,
            areas:
              item.areas.map(
                mapAreaScheduleCoverage,
              ),
          }),
        ),
    };
  },

  async saveGeneralSchedule(input: {
    id?: number;
    collectionAreaId: number;
    wasteType: CollectionWasteType;
    emissionDays: string;
    startTime?: string;
    endTime?: string;
  }) {
    if (CONFIG.useMocks) {
      await delay();
      return;
    }

    const body = {
      emissionDays:
        input.emissionDays.trim(),
      startTime:
        input.startTime || null,
      endTime:
        input.endTime || null,
    };

    if (input.id) {
      await request(
        ENDPOINTS.collectionAreaSchedule(
          input.id,
        ),
        'PATCH',
        body,
      );
      return;
    }

    await request(
      ENDPOINTS.collectionAreaSchedules,
      'POST',
      {
        collectionAreaId:
          input.collectionAreaId,
        wasteType:
          input.wasteType,
        ...body,
      },
    );
  },

  async deleteGeneralSchedule(
    id: number,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      return;
    }

    await request(
      ENDPOINTS.collectionAreaSchedule(
        id,
      ),
      'DELETE',
    );
  },

  async apartmentSchedules(
    apartmentId: number,
  ): Promise<ApartmentSchedule[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [];
    }

    const data =
      await request<unknown>(
        `${ENDPOINTS.apartmentSchedules}?apartmentId=${apartmentId}`,
      );

    return pageItems<
      BackendApartmentSchedule
    >(data).map(
      (item) => ({
        id: item.id,
        apartmentId:
          item.apartmentId,
        apartmentName:
          item.apartmentName,
        wasteItemId:
          item.wasteItem.id,
        wasteItemName:
          item.wasteItem.name,
        dayOfWeek:
          item.dayOfWeek,
        startTime:
          item.startTime?.slice(
            0,
            5,
          ) ?? undefined,
        endTime:
          item.endTime?.slice(
            0,
            5,
          ) ?? undefined,
        alwaysAvailable:
          item.alwaysAvailable,
        createdAt:
          formatDate(
            item.createdAt,
          ),
        updatedAt:
          formatDate(
            item.updatedAt,
          ),
      }),
    );
  },

  async saveApartmentSchedule(input: {
    id?: number;
    apartmentId: number;
    wasteItemId: number;
    dayOfWeek: string;
    startTime?: string;
    endTime?: string;
    alwaysAvailable: boolean;
  }) {
    if (CONFIG.useMocks) {
      await delay();
      return;
    }

    const body = {
      dayOfWeek:
        input.dayOfWeek,
      startTime:
        input.alwaysAvailable
          ? null
          : input.startTime ||
            null,
      endTime:
        input.alwaysAvailable
          ? null
          : input.endTime ||
            null,
      alwaysAvailable:
        input.alwaysAvailable,
    };

    if (input.id) {
      await request(
        ENDPOINTS.apartmentSchedule(
          input.id,
        ),
        'PATCH',
        body,
      );
      return;
    }

    await request(
      ENDPOINTS.apartmentSchedules,
      'POST',
      {
        apartmentId:
          input.apartmentId,
        wasteItemId:
          input.wasteItemId,
        ...body,
      },
    );
  },

  async deleteApartmentSchedule(
    id: number,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      return;
    }

    await request(
      ENDPOINTS.apartmentSchedule(
        id,
      ),
      'DELETE',
    );
  },

  async schedules(): Promise<Schedule[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [...schedules];
    }

    const data = await request<unknown>(
      `${ENDPOINTS.collectionAreaSchedules}?size=200`,
    );
    const backendItems = pageItems<BackendCollectionAreaSchedule>(data);

    scheduleMeta.clear();
    backendItems.forEach((item) => scheduleMeta.set(item.id, item));

    return backendItems.map((item) => ({
      id: item.id,
      area: item.collectionAreaName,
      category: wasteTypeLabel(item.wasteType),
      day: item.emissionDays,
      time: scheduleTime(item.startTime, item.endTime),
      note: [item.emissionMethod, item.emissionPlace, item.uncollectedDay]
        .filter(Boolean)
        .join(' · '),
      active: true,
    }));
  },

  async saveSchedule(
    item: Partial<Schedule> & Pick<Schedule, 'area' | 'category' | 'day' | 'time'>,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) {
        schedules = schedules.map((schedule) =>
          schedule.id === item.id ? { ...schedule, ...item } as Schedule : schedule,
        );
      } else {
        schedules = [
          ...schedules,
          {
            id: Date.now(),
            area: item.area,
            category: item.category,
            day: item.day,
            time: item.time,
            note: item.note ?? '',
            active: item.active ?? true,
          },
        ];
      }
      return;
    }

    const { startTime, endTime } = parseTimeRange(item.time);

    if (item.id) {
      if (!scheduleMeta.has(item.id)) {
        throw new Error('수정할 일정 원본 정보를 다시 불러와주세요.');
      }

      await request(ENDPOINTS.collectionAreaSchedule(item.id), 'PATCH', {
        emissionDays: item.day,
        startTime,
        endTime,
      });
      return;
    }

    const area = await findCollectionAreaByName(item.area);

    await request(ENDPOINTS.collectionAreaSchedules, 'POST', {
      collectionAreaId: area.id,
      wasteType: uiCategoryToWasteType(item.category),
      emissionDays: item.day,
      startTime,
      endTime,
    });
  },

  async wasteItems(keyword = ''): Promise<WasteItem[]> {
    if (CONFIG.useMocks) {
      await delay();
      const q = keyword.toLowerCase().trim();
      return wasteItems.filter(
        (item) =>
          !q ||
          [item.name, item.category, ...item.aliases].some((value) =>
            value.toLowerCase().includes(q),
          ),
      );
    }

    const data = await request<unknown>(
      `${ENDPOINTS.wasteItems}?keyword=${encodeURIComponent(keyword)}&size=200`,
    );
    const backendItems = pageItems<BackendWasteItem>(data);

    backendItems.forEach((item) => wasteItemMeta.set(item.id, item));

    return backendItems.map((item) => ({
      id: item.id,
      name: item.name,
      category: item.category.name,
      aliases: (item.searchKeywords ?? '')
        .split(',')
        .map((value) => value.trim())
        .filter(Boolean),
      aiSupported: AI_SUPPORTED_ITEM_NAMES.has(item.name),
      active: item.active,
    }));
  },

  async saveWasteItem(
    item: Partial<WasteItem> & Pick<WasteItem, 'name' | 'category'>,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) {
        wasteItems = wasteItems.map((wasteItem) =>
          wasteItem.id === item.id
            ? { ...wasteItem, ...item, aliases: item.aliases ?? wasteItem.aliases } as WasteItem
            : wasteItem,
        );
      } else {
        wasteItems = [
          ...wasteItems,
          {
            id: Date.now(),
            name: item.name,
            category: item.category,
            aliases: item.aliases ?? [],
            aiSupported: item.aiSupported ?? false,
            active: item.active ?? true,
          },
        ];
      }
      return;
    }

    const category = await findWasteCategory(item.category);
    const existing = item.id ? wasteItemMeta.get(item.id) : undefined;

    const body = {
      categoryId: category.id,
      name: item.name,
      searchKeywords: (item.aliases ?? []).join(','),
      imageUrl: existing?.imageUrl ?? null,
    };

    await request(
      item.id ? ENDPOINTS.wasteItem(item.id) : ENDPOINTS.wasteItems,
      item.id ? 'PATCH' : 'POST',
      body,
    );
  },

  async guides(): Promise<Guide[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [...guides];
    }

    const items = await adminApi.wasteItems('');

    const results = await Promise.all(
      items
        .filter((item) => item.active)
        .map(async (item): Promise<Guide | null> => {
          try {
            const detail = await request<BackendWasteDetail>(
              ENDPOINTS.wasteItemDetail(item.id),
            );

            if (!detail.guide) return null;

            return {
              id: detail.guide.id,
              wasteItem: detail.name,
              summary: detail.guide.summary,
              method: detail.guide.disposalMethod,
              caution: detail.guide.caution ?? '',
              checks: detail.guide.checkItems
                .sort((a, b) => a.sortOrder - b.sortOrder)
                .map((check) => check.content),
            };
          } catch {
            return null;
          }
        }),
    );

    return results.filter((item): item is Guide => item !== null);
  },

  async saveGuide(
    item: Partial<Guide> & Pick<Guide, 'wasteItem' | 'summary' | 'method' | 'caution'>,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) {
        guides = guides.map((guide) =>
          guide.id === item.id
            ? { ...guide, ...item, checks: item.checks ?? guide.checks } as Guide
            : guide,
        );
      } else {
        guides = [
          ...guides,
          {
            id: Date.now(),
            wasteItem: item.wasteItem,
            summary: item.summary,
            method: item.method,
            caution: item.caution,
            checks: item.checks ?? [],
          },
        ];
      }
      return;
    }

    const allItems = await adminApi.wasteItems(item.wasteItem);
    const wasteItem = allItems.find(
      (candidate) =>
        candidate.name.trim().toLowerCase() === item.wasteItem.trim().toLowerCase(),
    );

    if (!wasteItem) {
      throw new Error(`품목 '${item.wasteItem}'을 찾지 못했습니다.`);
    }

    const checks = item.checks ?? [];
    if (checks.length === 0) {
      throw new Error('체크리스트를 1개 이상 입력해주세요.');
    }

    await request(ENDPOINTS.wasteItemGuide(wasteItem.id), 'PUT', {
      summary: item.summary,
      disposalMethod: item.method,
      caution: item.caution,
      checkItems: checks.map((content, index) => ({
        content,
        sortOrder: index,
        required: true,
      })),
    });
  },

  async corrections(status: ReviewStatus | 'ALL'): Promise<AiCorrection[]> {
    if (CONFIG.useMocks) {
      await delay();
      return corrections.filter((item) => status === 'ALL' || item.status === status);
    }

    const query = status === 'ALL' ? '' : `?status=${status}&size=200`;
    const allQuery = status === 'ALL' ? '?size=200' : query;
    const data = await request<unknown>(`${ENDPOINTS.aiCorrections}${allQuery}`);

    return pageItems<BackendCorrection>(data).map((item) => ({
      id: item.id,
      imageLogId: item.imageLogId,
      userEmail: item.userEmail,
      aiItem: item.aiWasteItemName ?? '판별 실패',
      aiConfidence: item.aiConfidence ?? 0,
      correctedItem: item.correctedWasteItemName ?? '-',
      status: item.reviewStatus,
      correctedAt: formatDate(item.correctedAt),
      memo: item.reviewMemo ?? undefined,
    }));
  },

  async reviewCorrection(id: number, approved: boolean, memo = '') {
    if (CONFIG.useMocks) {
      await delay();
      corrections = corrections.map((item) =>
        item.id === id
          ? { ...item, status: approved ? 'APPROVED' : 'REJECTED', memo }
          : item,
      );
      return;
    }

    await request(
      approved ? ENDPOINTS.approveAi(id) : ENDPOINTS.rejectAi(id),
      'POST',
      { memo },
    );
  },

  async syncLogs(): Promise<SyncLog[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [...syncLogs];
    }

    const data = await request<unknown>(`${ENDPOINTS.publicDataLogs}?size=100`);

    return pageItems<BackendSyncLog>(data).map((item) => ({
      id: item.id,
      source: item.source,
      status: item.status,
      inserted: item.insertedCount,
      updated: item.updatedCount,
      failed: item.failedCount,
      at: formatDate(item.finishedAt ?? item.startedAt),
    }));
  },

  async syncPublicData() {
    if (CONFIG.useMocks) {
      await new Promise((resolve) => setTimeout(resolve, 700));
      syncLogs = [
        {
          id: Date.now(),
          source: '부산진구 생활폐기물 공공데이터',
          status: 'SUCCESS',
          inserted: 0,
          updated: 16,
          failed: 0,
          at: new Date().toLocaleString('ko-KR'),
        },
        ...syncLogs,
      ];
      return;
    }

    await request(ENDPOINTS.publicDataSync, 'POST');
  },

  async notifications(): Promise<Notification[]> {
    if (CONFIG.useMocks) {
      await delay();
      return [...notifications];
    }

    const data = await request<unknown>(
      `${ENDPOINTS.notifications}?size=200`,
    );

    return pageItems<BackendNotification>(data).map((item) => ({
      id: item.id,
      title: item.title,
      body: item.body,
      target: notificationTargetLabel(item.targetType),
      status: item.status,
      sentAt: formatDate(item.sentAt),
    }));
  },

  async createNotification(
    item: Pick<Notification, 'title' | 'body' | 'target'>,
  ) {
    if (CONFIG.useMocks) {
      await delay();
      notifications = [
        {
          id: Date.now(),
          ...item,
          status: 'SENT',
          sentAt: new Date().toLocaleString('ko-KR'),
        },
        ...notifications,
      ];
      return;
    }

    await request(ENDPOINTS.notifications, 'POST', {
      title: item.title,
      body: item.body,
      targetType: notificationTargetValue(item.target),
    });
  },

  async cancelNotification(id: number) {
    if (CONFIG.useMocks) {
      await delay();
      notifications = notifications.map((item) =>
        item.id === id ? { ...item, status: 'CANCELLED' } : item,
      );
      return;
    }

    await request(ENDPOINTS.cancelNotification(id), 'PATCH');
  },

};

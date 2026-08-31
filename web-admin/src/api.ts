import { CONFIG, ENDPOINTS } from './config';
import {
  initialAreas, initialCorrections, initialDashboard, initialGuides,
  initialNotifications, initialResidences, initialSchedules,
  initialSyncLogs, initialUsers, initialWasteItems
} from './mockData';
import type {
  AiCorrection, CollectionArea, Dashboard, Guide, Notification,
  Residence, ReviewStatus, Schedule, SyncLog, User, UserStatus, WasteItem
} from './types';

const ACCESS = 'smartrecycle_admin_access';
const REFRESH = 'smartrecycle_admin_refresh';
const SESSION = 'smartrecycle_admin_session';

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

async function request<T>(path: string, method: Method = 'GET', body?: unknown): Promise<T> {
  const token = localStorage.getItem(ACCESS);
  const response = await fetch(`${CONFIG.apiBaseUrl}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(SESSION);
    window.dispatchEvent(new Event('smartrecycle-auth-expired'));
    throw new Error('로그인이 만료되었습니다.');
  }

  const raw = await response.text();
  const parsed = raw ? JSON.parse(raw) : null;

  if (!response.ok) {
    throw new Error(parsed?.message ?? `요청 실패 (${response.status})`);
  }

  return (parsed?.data ?? parsed) as T;
}

function pageItems<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  if (value && typeof value === 'object') {
    const v = value as Record<string, unknown>;
    if (Array.isArray(v.content)) return v.content as T[];
    if (Array.isArray(v.items)) return v.items as T[];
  }
  return [];
}

function delay() {
  return new Promise<void>(resolve => setTimeout(resolve, 150));
}

let users = [...initialUsers];
let residences = [...initialResidences];
let areas = [...initialAreas];
let schedules = [...initialSchedules];
let wasteItems = [...initialWasteItems];
let guides = [...initialGuides];
let corrections = [...initialCorrections];
let syncLogs = [...initialSyncLogs];
let notifications = [...initialNotifications];

export const authApi = {
  session() {
    const raw = localStorage.getItem(SESSION);
    return raw ? JSON.parse(raw) as { email: string; role: string } : null;
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

    const data = await request<any>(ENDPOINTS.login, 'POST', { email, password });
    if (!data?.accessToken || !data?.refreshToken) throw new Error('로그인 토큰을 확인하지 못했습니다.');
    if (data.role && data.role !== 'ADMIN') throw new Error('관리자 권한이 필요합니다.');
    localStorage.setItem(ACCESS, data.accessToken);
    localStorage.setItem(REFRESH, data.refreshToken);
    const session = { email: data.email ?? email, role: data.role ?? 'ADMIN' };
    localStorage.setItem(SESSION, JSON.stringify(session));
    return session;
  },
  logout() {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(SESSION);
  },
};

export const adminApi = {
  async dashboard(): Promise<Dashboard> {
    if (CONFIG.useMocks) {
      await delay();
      return {
        ...initialDashboard,
        users: users.length,
        activeUsers: users.filter(x => x.status === 'ACTIVE').length,
        pendingResidences: residences.filter(x => x.approval === 'PENDING').length,
        wasteItems: wasteItems.length,
        pendingAi: corrections.filter(x => x.status === 'PENDING').length,
        scheduledNotifications: notifications.filter(x => x.status === 'SCHEDULED').length,
      };
    }
    return request<Dashboard>(ENDPOINTS.dashboard);
  },

  async users(keyword = ''): Promise<User[]> {
    if (CONFIG.useMocks) {
      await delay();
      const q = keyword.toLowerCase().trim();
      return users.filter(x => !q || [x.email, x.name, x.residence, x.address].some(v => v.toLowerCase().includes(q)));
    }
    return pageItems<User>(await request<unknown>(`${ENDPOINTS.users}?keyword=${encodeURIComponent(keyword)}&size=200`));
  },
  async setUserStatus(id: number, status: UserStatus) {
    if (CONFIG.useMocks) {
      await delay(); users = users.map(x => x.id === id ? { ...x, status } : x); return;
    }
    await request(ENDPOINTS.userStatus(id), 'PATCH', { status });
  },
  async setUserRole(id: number, role: 'USER' | 'ADMIN') {
    if (CONFIG.useMocks) {
      await delay(); users = users.map(x => x.id === id ? { ...x, role } : x); return;
    }
    await request(ENDPOINTS.userRole(id), 'PATCH', { role });
  },

  async residences(): Promise<Residence[]> {
    if (CONFIG.useMocks) { await delay(); return [...residences]; }
    return pageItems<Residence>(await request<unknown>(`${ENDPOINTS.residences}?size=200`));
  },
  async reviewResidence(id: number, approved: boolean) {
    if (CONFIG.useMocks) {
      await delay(); residences = residences.map(x => x.id === id ? { ...x, approval: approved ? 'APPROVED' : 'REJECTED' } : x); return;
    }
    await request(approved ? ENDPOINTS.approveResidence(id) : ENDPOINTS.rejectResidence(id), 'POST');
  },

  async areas(): Promise<CollectionArea[]> {
    if (CONFIG.useMocks) { await delay(); return [...areas]; }
    return pageItems<CollectionArea>(await request<unknown>(`${ENDPOINTS.collectionAreas}?size=200`));
  },
  async saveArea(item: Partial<CollectionArea> & { name: string; district: string }) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) areas = areas.map(x => x.id === item.id ? { ...x, ...item, dongs: item.dongs ?? x.dongs } as CollectionArea : x);
      else areas = [...areas, { id: Date.now(), name: item.name, district: item.district, dongs: item.dongs ?? [], active: item.active ?? true, updatedAt: new Date().toLocaleString('ko-KR') }];
      return;
    }
    await request(item.id ? ENDPOINTS.collectionArea(item.id) : ENDPOINTS.collectionAreas, item.id ? 'PUT' : 'POST', item);
  },

  async schedules(): Promise<Schedule[]> {
    if (CONFIG.useMocks) { await delay(); return [...schedules]; }
    return pageItems<Schedule>(await request<unknown>(`${ENDPOINTS.schedules}?size=200`));
  },
  async saveSchedule(item: Partial<Schedule> & Pick<Schedule, 'area' | 'category' | 'day' | 'time'>) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) schedules = schedules.map(x => x.id === item.id ? { ...x, ...item } as Schedule : x);
      else schedules = [...schedules, { id: Date.now(), area: item.area, category: item.category, day: item.day, time: item.time, note: item.note ?? '', active: item.active ?? true }];
      return;
    }
    await request(item.id ? ENDPOINTS.schedule(item.id) : ENDPOINTS.schedules, item.id ? 'PUT' : 'POST', item);
  },

  async wasteItems(keyword = ''): Promise<WasteItem[]> {
    if (CONFIG.useMocks) {
      await delay(); const q = keyword.toLowerCase().trim();
      return wasteItems.filter(x => !q || [x.name, x.category, ...x.aliases].some(v => v.toLowerCase().includes(q)));
    }
    return pageItems<WasteItem>(await request<unknown>(`${ENDPOINTS.wasteItems}?keyword=${encodeURIComponent(keyword)}&size=200`));
  },
  async saveWasteItem(item: Partial<WasteItem> & Pick<WasteItem, 'name' | 'category'>) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) wasteItems = wasteItems.map(x => x.id === item.id ? { ...x, ...item, aliases: item.aliases ?? x.aliases } as WasteItem : x);
      else wasteItems = [...wasteItems, { id: Date.now(), name: item.name, category: item.category, aliases: item.aliases ?? [], aiSupported: item.aiSupported ?? false, active: item.active ?? true }];
      return;
    }
    await request(item.id ? ENDPOINTS.wasteItem(item.id) : ENDPOINTS.wasteItems, item.id ? 'PUT' : 'POST', item);
  },

  async guides(): Promise<Guide[]> {
    if (CONFIG.useMocks) { await delay(); return [...guides]; }
    return pageItems<Guide>(await request<unknown>(`${ENDPOINTS.guides}?size=200`));
  },
  async saveGuide(item: Partial<Guide> & Pick<Guide, 'wasteItem' | 'summary' | 'method' | 'caution'>) {
    if (CONFIG.useMocks) {
      await delay();
      if (item.id) guides = guides.map(x => x.id === item.id ? { ...x, ...item, checks: item.checks ?? x.checks } as Guide : x);
      else guides = [...guides, { id: Date.now(), wasteItem: item.wasteItem, summary: item.summary, method: item.method, caution: item.caution, checks: item.checks ?? [] }];
      return;
    }
    await request(item.id ? ENDPOINTS.guide(item.id) : ENDPOINTS.guides, item.id ? 'PUT' : 'POST', item);
  },

  async corrections(status: ReviewStatus | 'ALL'): Promise<AiCorrection[]> {
    if (CONFIG.useMocks) { await delay(); return corrections.filter(x => status === 'ALL' || x.status === status); }
    return pageItems<AiCorrection>(await request<unknown>(`${ENDPOINTS.aiCorrections}?status=${status === 'ALL' ? '' : status}&size=200`));
  },
  async reviewCorrection(id: number, approved: boolean, memo = '') {
    if (CONFIG.useMocks) {
      await delay(); corrections = corrections.map(x => x.id === id ? { ...x, status: approved ? 'APPROVED' : 'REJECTED', memo } : x); return;
    }
    await request(approved ? ENDPOINTS.approveAi(id) : ENDPOINTS.rejectAi(id), 'POST', { memo });
  },

  async syncLogs(): Promise<SyncLog[]> {
    if (CONFIG.useMocks) { await delay(); return [...syncLogs]; }
    return pageItems<SyncLog>(await request<unknown>(`${ENDPOINTS.publicDataLogs}?size=100`));
  },
  async syncPublicData() {
    if (CONFIG.useMocks) {
      await new Promise(r => setTimeout(r, 700));
      syncLogs = [{ id: Date.now(), source: '부산진구 생활폐기물 공공데이터', status: 'SUCCESS', inserted: 0, updated: 16, failed: 0, at: new Date().toLocaleString('ko-KR') }, ...syncLogs];
      return;
    }
    await request(ENDPOINTS.publicDataSync, 'POST');
  },

  async notifications(): Promise<Notification[]> {
    if (CONFIG.useMocks) { await delay(); return [...notifications]; }
    return pageItems<Notification>(await request<unknown>(`${ENDPOINTS.notifications}?size=200`));
  },
  async createNotification(item: Pick<Notification, 'title' | 'body' | 'target'> & { at?: string }) {
    if (CONFIG.useMocks) {
      await delay();
      notifications = [{ id: Date.now(), ...item, status: item.at ? 'SCHEDULED' : 'DRAFT' }, ...notifications];
      return;
    }
    await request(ENDPOINTS.notifications, 'POST', item);
  },
  async sendNotification(id: number) {
    if (CONFIG.useMocks) {
      await delay(); notifications = notifications.map(x => x.id === id ? { ...x, status: 'SENT', at: new Date().toLocaleString('ko-KR') } : x); return;
    }
    await request(ENDPOINTS.notificationSend(id), 'POST');
  },
};

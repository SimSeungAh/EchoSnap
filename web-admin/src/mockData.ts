import type {
  AiCorrection, CollectionArea, Dashboard, Guide, Notification,
  Residence, Schedule, SyncLog, User, WasteItem
} from './types';

export const initialUsers: User[] = [
  { id: 1, email: 'user1@smartrecycle.com', name: '김사용', role: 'USER', status: 'ACTIVE', residence: '스마트아파트', address: '부산광역시 부산진구 중앙대로 100', createdAt: '2026-08-02' },
  { id: 2, email: 'house@smartrecycle.com', name: '이주택', role: 'USER', status: 'ACTIVE', residence: '일반주택', address: '부산광역시 부산진구 부전동 123-4', createdAt: '2026-08-08' },
  { id: 3, email: 'sleep@smartrecycle.com', name: '박휴면', role: 'USER', status: 'WITHDRAWN', residence: '센트럴 오피스텔', address: '부산광역시 부산진구 가야대로 55', createdAt: '2026-07-20' },
];

export const initialResidences: Residence[] = [
  { id: 1, name: '스마트아파트', type: 'APARTMENT', address: '부산광역시 부산진구 중앙대로 100', buildingNo: 'B-2026-0001', area: '부전1동 A구역', approval: 'APPROVED' },
  { id: 2, name: '센트럴 오피스텔', type: 'OFFICETEL', address: '부산광역시 부산진구 가야대로 55', buildingNo: 'B-2026-0002', approval: 'PENDING' },
  { id: 3, name: '일반주택', type: 'DETACHED_HOUSE', address: '부산광역시 부산진구 부전로 22', area: '부전1동 B구역', approval: 'APPROVED' },
];

export const initialAreas: CollectionArea[] = [
  { id: 1, name: '부전1동 A구역', district: '부산진구', dongs: ['부전1동'], active: true, updatedAt: '2026-08-31 07:20' },
  { id: 2, name: '부전1동 B구역', district: '부산진구', dongs: ['부전1동'], active: true, updatedAt: '2026-08-31 07:20' },
  { id: 3, name: '부전2동 통합구역', district: '부산진구', dongs: ['부전2동'], active: true, updatedAt: '2026-08-30 15:00' },
];

export const initialSchedules: Schedule[] = [
  { id: 1, area: '부전1동 A구역', category: '종이류', day: '월요일', time: '18:00 ~ 21:00', note: '문전 배출', active: true },
  { id: 2, area: '부전1동 A구역', category: '플라스틱류', day: '수요일', time: '18:00 ~ 21:00', note: '', active: true },
  { id: 3, area: '부전1동 B구역', category: '폐전지·전자제품', day: '상시', time: '지정 수거함', note: '전용 수거함 이용', active: true },
];

export const initialWasteItems: WasteItem[] = [
  { id: 1, name: '종이박스', category: '종이류', aliases: ['박스', '골판지'], aiSupported: true, active: true },
  { id: 2, name: '페트병', category: '플라스틱류', aliases: ['PET', '투명페트병'], aiSupported: true, active: true },
  { id: 3, name: '플라스틱 용기', category: '플라스틱류', aliases: ['플라스틱통'], aiSupported: true, active: true },
  { id: 4, name: '캔', category: '금속류', aliases: ['음료캔', '통조림캔', '금속통'], aiSupported: true, active: true },
  { id: 5, name: '유리병', category: '유리류', aliases: ['병'], aiSupported: true, active: true },
  { id: 6, name: '스티로폼', category: '스티로폼', aliases: ['EPS'], aiSupported: true, active: true },
  { id: 7, name: '폐건전지', category: '폐전지·전자제품', aliases: ['건전지', '배터리'], aiSupported: false, active: true },
  { id: 8, name: '전선·케이블', category: '폐전지·전자제품', aliases: ['전선', '케이블', 'USB선'], aiSupported: false, active: true },
  { id: 9, name: '마우스', category: '폐전지·전자제품', aliases: ['컴퓨터 마우스'], aiSupported: false, active: true },
  { id: 10, name: '충전기·어댑터', category: '폐전지·전자제품', aliases: ['충전기', '어댑터'], aiSupported: false, active: true },
  { id: 11, name: '키보드', category: '폐전지·전자제품', aliases: ['컴퓨터 키보드'], aiSupported: false, active: true },
];

export const initialGuides: Guide[] = [
  { id: 1, wasteItem: '캔', summary: '내용물을 비우고 깨끗이 헹군 뒤 배출', method: '내용물을 완전히 비우고 가능한 경우 물로 헹군 뒤 금속류로 분리배출합니다.', caution: '담배꽁초 등 이물질을 넣지 말고 다른 재질은 분리합니다.', checks: ['내용물을 비웠나요?', '이물질을 제거했나요?', '다른 재질을 분리했나요?'] },
  { id: 2, wasteItem: '폐건전지', summary: '일반 종량제 봉투에 버리지 않고 전용 수거함 이용', method: '공동주택, 주민센터, 폐건전지 전용 수거함 등 지정 수거처에 배출합니다.', caution: '충전식 배터리는 단자부 합선에 주의합니다.', checks: ['전용 수거함 위치를 확인했나요?', '배터리 단자를 보호했나요?'] },
];

export const initialCorrections: AiCorrection[] = [
  { id: 1, imageLogId: 31, userEmail: 'user1@smartrecycle.com', aiItem: '종이박스', aiConfidence: 0.803, correctedItem: '캔', status: 'PENDING', correctedAt: '2026-08-31 15:20' },
  { id: 2, imageLogId: 25, userEmail: 'house@smartrecycle.com', aiItem: '플라스틱 용기', aiConfidence: 0.61, correctedItem: '페트병', status: 'APPROVED', correctedAt: '2026-08-30 19:10' },
];

export const initialSyncLogs: SyncLog[] = [
  { id: 1, source: '부산진구 생활폐기물 공공데이터', status: 'SUCCESS', inserted: 2, updated: 14, failed: 0, at: '2026-08-31 06:00' },
  { id: 2, source: '수거구역 데이터', status: 'SUCCESS', inserted: 0, updated: 16, failed: 0, at: '2026-08-30 06:00' },
];

export const initialNotifications: Notification[] = [
  { id: 1, title: '오늘은 플라스틱 배출일이에요', body: '오후 6시부터 9시 사이에 지정된 장소에 배출해주세요.', target: '일반주택 사용자', status: 'SENT', sentAt: '2026-08-31 17:30' },
  { id: 2, title: '분리배출 일정이 갱신되었습니다', body: '내 거주지의 최신 배출 일정을 확인해주세요.', target: '전체 사용자', status: 'SENT', sentAt: '2026-08-30 11:00' },
];

export const initialDashboard: Dashboard = {
  users: initialUsers.length,
  activeUsers: initialUsers.filter(x => x.status === 'ACTIVE').length,
  pendingResidences: initialResidences.filter(x => x.approval === 'PENDING').length,
  wasteItems: initialWasteItems.length,
  pendingAi: initialCorrections.filter(x => x.status === 'PENDING').length,
  todayNotifications: initialNotifications.filter(x => x.status === 'SENT').length,
};

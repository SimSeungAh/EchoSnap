export const CONFIG = {
  apiBaseUrl:
    import.meta.env.VITE_API_BASE_URL?.trim() ||
    'http://localhost:8080',
  useMocks:
    (import.meta.env.VITE_ADMIN_USE_MOCKS ?? 'true')
      .trim()
      .toLowerCase() === 'true',
};

export const ENDPOINTS = {
  login: '/api/auth/login',
  reissue: '/api/auth/reissue',
  dashboard: '/api/admin/dashboard',
  users: '/api/admin/users',
  userStatus: (id: number) => `/api/admin/users/${id}/status`,
  userRole: (id: number) => `/api/admin/users/${id}/role`,
  residences: '/api/admin/residences',
  approveResidence: (id: number) => `/api/admin/residences/${id}/approve`,
  rejectResidence: (id: number) => `/api/admin/residences/${id}/reject`,
  collectionAreas: '/api/admin/collection-areas',
  collectionArea: (id: number) => `/api/admin/collection-areas/${id}`,
  collectionAreaSync: '/api/admin/collection-areas/public-data/sync',
  schedules: '/api/admin/schedules',
  schedule: (id: number) => `/api/admin/schedules/${id}`,
  wasteItems: '/api/admin/waste/items',
  wasteItem: (id: number) => `/api/admin/waste/items/${id}`,
  guides: '/api/admin/waste/guides',
  guide: (id: number) => `/api/admin/waste/guides/${id}`,
  aiCorrections: '/api/admin/ai-corrections',
  approveAi: (id: number) => `/api/admin/ai-corrections/${id}/approve`,
  rejectAi: (id: number) => `/api/admin/ai-corrections/${id}/reject`,
  publicDataLogs: '/api/admin/public-data/sync-logs',
  publicDataSync: '/api/admin/public-data/sync',
  notifications: '/api/admin/notifications',
  notificationSend: (id: number) => `/api/admin/notifications/${id}/send`,
};

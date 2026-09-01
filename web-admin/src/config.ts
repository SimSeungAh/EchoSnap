export const CONFIG = {
  apiBaseUrl:
    import.meta.env.VITE_API_BASE_URL?.trim() ||
    'http://localhost:8080',

  useMocks:
    (import.meta.env.VITE_ADMIN_USE_MOCKS ?? 'false')
      .trim()
      .toLowerCase() === 'true',
};

export const ENDPOINTS = {
  login: '/api/auth/login',
  reissue: '/api/auth/reissue',
  dashboard: '/api/admin/dashboard',

  users: '/api/admin/users',
  user: (id: number) => `/api/admin/users/${id}`,
  userStatus: (id: number) => `/api/admin/users/${id}/status`,
  userRole: (id: number) => `/api/admin/users/${id}/role`,

  apartments: '/api/admin/apartments',
  apartment: (id: number) => `/api/admin/apartments/${id}`,
  approveApartment: (id: number) => `/api/admin/apartments/${id}/approve`,
  rejectApartment: (id: number) => `/api/admin/apartments/${id}/reject`,

  collectionAreas: '/api/admin/collection-areas',
  collectionArea: (id: number) => `/api/admin/collection-areas/${id}`,
  collectionAreaActivate: (id: number) => `/api/admin/collection-areas/${id}/activate`,
  collectionAreaDeactivate: (id: number) => `/api/admin/collection-areas/${id}/deactivate`,

  apartmentSchedules: '/api/admin/schedules',
  apartmentSchedule: (id: number) => `/api/admin/schedules/${id}`,

  collectionAreaSchedules: '/api/admin/collection-area-schedules',
  collectionAreaSchedule: (id: number) => `/api/admin/collection-area-schedules/${id}`,

  wasteCategories: '/api/admin/waste/categories',
  wasteCategory: (id: number) => `/api/admin/waste/categories/${id}`,
  wasteCategoryDeactivate: (id: number) => `/api/admin/waste/categories/${id}/deactivate`,

  wasteItems: '/api/admin/waste/items',
  wasteItem: (id: number) => `/api/admin/waste/items/${id}`,
  wasteItemDeactivate: (id: number) => `/api/admin/waste/items/${id}/deactivate`,
  wasteItemGuide: (id: number) => `/api/admin/waste/items/${id}/guide`,
  wasteItemDetail: (id: number) => `/api/waste/items/${id}`,

  aiCorrections: '/api/admin/ai-corrections',
  aiCorrection: (id: number) => `/api/admin/ai-corrections/${id}`,
  aiCorrectionImage: (id: number) => `/api/admin/ai-corrections/${id}/image`,
  approveAi: (id: number) => `/api/admin/ai-corrections/${id}/approve`,
  rejectAi: (id: number) => `/api/admin/ai-corrections/${id}/reject`,

  publicDataLogs: '/api/admin/public-data/sync-logs',
  publicDataSync: '/api/admin/public-data/sync',

  notifications: '/api/admin/notifications',
  cancelNotification: (id: number) => `/api/admin/notifications/${id}/cancel`,
};

# 관리자 백엔드 API 연결표

프론트는 전체 구현되어 있고 API 경로는 `src/config.ts`에 집중시켰습니다.

예상 API:
- POST `/api/auth/login`
- POST `/api/auth/reissue`
- GET `/api/admin/dashboard`
- GET/PATCH `/api/admin/users/**`
- GET/POST `/api/admin/residences/**`
- GET/POST/PUT `/api/admin/collection-areas/**`
- GET/POST/PUT `/api/admin/schedules/**`
- GET/POST/PUT `/api/admin/waste/items/**`
- GET/POST/PUT `/api/admin/waste/guides/**`
- GET/POST `/api/admin/ai-corrections/**`
- GET/POST `/api/admin/public-data/**`
- GET/POST `/api/admin/notifications/**`

학원에서 Swagger 기준 실제 URL이 다르면 `ENDPOINTS`만 조정하세요.

AI 정정 데이터:
```text
AI 원본 예측
+ 사용자 정정
→ 관리자 PENDING 검수
→ APPROVED / REJECTED
→ 승인 데이터만 재학습 후보
```

# SmartRecycle Web Admin

SmartRecycle 관리자 웹 전체 구현본입니다.

## 포함 기능
- 관리자 로그인 / 인증 가드
- 대시보드
- 사용자 관리
- 거주지 승인 관리
- 일반주택 수거구역 관리
- 배출 일정 관리
- 폐기물 품목 CRUD
- 분리배출 가이드 CRUD
- AI 사용자 정정 검수 (PENDING / APPROVED / REJECTED)
- 공공데이터 동기화 관리
- 알림 작성 / 예약 / 즉시발송 UI
- 관리자 설정 / API 상태 확인
- 반응형 사이드바

## 실행
```powershell
cd C:\workspace\Smart-Recycle\web-admin
npm install
Copy-Item .env.example .env
npm run dev
```

기본 포트: `5174`

## 데모 모드
`.env`:
```text
VITE_ADMIN_USE_MOCKS=true
```

로그인:
```text
admin@smartrecycle.com
Admin1234!
```

## 실제 Spring Boot 연결
`.env`:
```text
VITE_API_BASE_URL=http://localhost:8080
VITE_ADMIN_USE_MOCKS=false
```

실제 관리자 API 경로가 다르면 `src/config.ts`의 `ENDPOINTS`만 우선 맞추면 됩니다.
응답 DTO가 다르면 `src/api.ts`의 unwrap / normalize 구간을 맞추면 됩니다.

> JWT Secret, 공공데이터 키, Kakao 키 등 비밀값은 절대 웹 프로젝트에 넣지 않습니다.

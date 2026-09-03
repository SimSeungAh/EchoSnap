# EchoSnap API 명세

## 1. 문서 정보

| 항목 | 내용 |
|---|---|
| 프로젝트 | EchoSnap |
| API 서버 | Spring Boot |
| 사용자 앱 | Flutter |
| 관리자 웹 | React + TypeScript |
| 기본 경로 | `/api` |
| 인증 방식 | JWT Bearer Token |
| 응답 형식 | `ApiResponse<T>` |
| 문서 버전 | 0.1 |

---

## 2. API 설계 원칙

### 2.1 플랫폼 역할

EchoSnap의 Flutter 사용자 앱과 React 관리자 웹은 동일한 Spring Boot API를 사용한다.

```text
Flutter 사용자 앱
    ↓
Spring Boot API
    ↓
MySQL / Redis / Python AI Server
    ↑
React 관리자 웹
```

- Flutter는 사용자 기능을 담당한다.
- React는 관리자 기능만 담당한다.
- 비즈니스 규칙은 Spring Boot에서 처리한다.
- Flutter와 React는 데이터베이스에 직접 접근하지 않는다.
- 관리자 API는 `/api/admin` 경로로 분리한다.

---

## 3. 인증 규칙

### 3.1 Authorization Header

인증이 필요한 API는 다음 헤더를 사용한다.

```http
Authorization: Bearer {accessToken}
```

### 3.2 권한

| 권한 | 설명 |
|---|---|
| `USER` | Flutter 일반 사용자 |
| `ADMIN` | React 관리자 웹 사용자 |

### 3.3 접근 범위

| API 경로 | 접근 권한 |
|---|---|
| `/api/auth/**` | 비로그인 허용 |
| `/api/addresses/**` | 비로그인 또는 로그인 |
| `/api/waste/**` 조회 | 로그인 사용자 |
| `/api/users/**` | 로그인 사용자 |
| `/api/apartments/**` | 로그인 사용자 |
| `/api/schedules/**` | 로그인 사용자 |
| `/api/schedule-proposals/**` | 로그인 사용자 |
| `/api/image-logs/**` | 로그인 사용자 |
| `/api/notifications/**` | 로그인 사용자 |
| `/api/admin/**` | `ADMIN`만 허용 |

---

## 4. 공통 응답

### 4.1 성공 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {}
}
```

데이터가 없는 성공 응답:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": null
}
```

### 4.2 실패 응답

```json
{
  "success": false,
  "code": "COMMON_001",
  "message": "잘못된 입력입니다.",
  "data": null
}
```

### 4.3 페이지 응답

목록 API는 다음 구조를 사용한다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "목록 조회 성공",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

---

# 5. 인증 API

현재 백엔드 템플릿에 구현되어 있는 API이다.

## 5.1 회원가입

```http
POST /api/auth/signup
```

### 권한

```text
비로그인 허용
```

### Request

```json
{
  "email": "user1@echosnap.com",
  "password": "test1234!",
  "nickname": "테스트사용자"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": null
}
```

### 오류

| 코드 | 상황 |
|---|---|
| `COMMON_001` | 입력값 검증 실패 |
| `USER_001` | 이미 사용 중인 이메일 |

---

## 5.2 로그인

```http
POST /api/auth/login
```

### 권한

```text
비로그인 허용
```

### Request

```json
{
  "email": "user1@echosnap.com",
  "password": "test1234!"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "access-token",
    "refreshToken": "refresh-token"
  }
}
```

### 오류

| 코드 | 상황 |
|---|---|
| `USER_002` | 사용자를 찾을 수 없음 |
| `USER_003` | 비밀번호 불일치 |

---

## 5.3 로그아웃

```http
POST /api/auth/logout
```

### 권한

```text
비로그인 허용
```

### Request

```json
{
  "refreshToken": "refresh-token"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "로그아웃되었습니다.",
  "data": null
}
```

---

## 5.4 토큰 재발급

```http
POST /api/auth/reissue
```

### 권한

```text
비로그인 허용
```

### Request

```json
{
  "refreshToken": "refresh-token"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token"
  }
}
```

### 오류

| 코드 | 상황 |
|---|---|
| `TOKEN_001` | 유효하지 않은 토큰 |
| `TOKEN_002` | 만료된 토큰 |
| `TOKEN_003` | 지원하지 않는 토큰 |
| `TOKEN_004` | 토큰이 없음 |

---

# 6. 사용자 API

## 6.1 내 정보 조회

현재 구현되어 있는 API이다.

```http
GET /api/users/me
```

### 권한

```text
USER 또는 ADMIN
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "내 정보 조회 성공",
  "data": {
    "id": 1,
    "email": "user1@echosnap.com",
    "nickname": "테스트사용자",
    "role": "USER",
    "status": "ACTIVE",
    "apartment": {
      "id": 1,
      "name": "스마트아파트",
      "roadAddress": "서울특별시 테스트구 테스트로 1"
    },
    "notificationEnabled": true,
    "locationEnabled": true,
    "onboardingCompleted": true
  }
}
```

아파트를 아직 설정하지 않은 경우:

```json
{
  "id": 1,
  "email": "user1@echosnap.com",
  "nickname": "테스트사용자",
  "role": "USER",
  "status": "ACTIVE",
  "apartment": null,
  "notificationEnabled": false,
  "locationEnabled": false,
  "onboardingCompleted": false
}
```

---

## 6.2 내 정보 수정

```http
PATCH /api/users/me
```

### Request

```json
{
  "nickname": "새로운닉네임"
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "내 정보가 수정되었습니다.",
  "data": {
    "id": 1,
    "email": "user1@echosnap.com",
    "nickname": "새로운닉네임"
  }
}
```

---

## 6.3 내 아파트 설정 또는 변경

```http
PATCH /api/users/me/apartment
```

### Request

```json
{
  "apartmentId": 1
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "거주 아파트가 설정되었습니다.",
  "data": {
    "apartmentId": 1,
    "apartmentName": "스마트아파트",
    "apartmentStatus": "APPROVED"
  }
}
```

### 제약사항

- 승인된 아파트만 일반 사용자가 선택할 수 있다.
- 존재하지 않는 아파트는 설정할 수 없다.
- 거절된 아파트는 설정할 수 없다.

---

## 6.4 사용자 설정 변경

```http
PATCH /api/users/me/settings
```

### Request

```json
{
  "notificationEnabled": true,
  "locationEnabled": true
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "사용자 설정이 변경되었습니다.",
  "data": {
    "notificationEnabled": true,
    "locationEnabled": true
  }
}
```

---

## 6.5 온보딩 완료 처리

```http
PATCH /api/users/me/onboarding
```

### Request

```json
{
  "completed": true
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "초기 설정이 완료되었습니다.",
  "data": {
    "onboardingCompleted": true
  }
}
```

---

# 7. 아파트 API

## 7.1 승인된 아파트 검색

```http
GET /api/apartments?keyword={keyword}&page={page}&size={size}
```

### 예시

```http
GET /api/apartments?keyword=스마트&page=0&size=20
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아파트 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "스마트아파트",
        "roadAddress": "서울특별시 테스트구 테스트로 1",
        "jibunAddress": "서울특별시 테스트구 테스트동 1",
        "latitude": 37.123456,
        "longitude": 127.123456,
        "status": "APPROVED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 조회 조건

일반 사용자 API에서는 다음 상태만 반환한다.

```text
APPROVED
```

---

## 7.2 아파트 상세 조회

```http
GET /api/apartments/{apartmentId}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아파트 상세 조회 성공",
  "data": {
    "id": 1,
    "name": "스마트아파트",
    "roadAddress": "서울특별시 테스트구 테스트로 1",
    "jibunAddress": "서울특별시 테스트구 테스트동 1",
    "buildingManagementNumber": "1234567890123456789012345",
    "latitude": 37.123456,
    "longitude": 127.123456,
    "status": "APPROVED"
  }
}
```

---

## 7.3 신축 아파트 임시 등록

주소 검색 결과에 아파트가 없는 경우 사용한다.

```http
POST /api/apartments/temporary
```

### Request

```json
{
  "name": "새로운아파트",
  "roadAddress": "서울특별시 테스트구 새길 10",
  "jibunAddress": "서울특별시 테스트구 새동 100",
  "buildingManagementNumber": "9999999999999999999999999",
  "latitude": 37.123456,
  "longitude": 127.123456
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아파트가 임시 등록되었습니다.",
  "data": {
    "id": 2,
    "name": "새로운아파트",
    "status": "PENDING"
  }
}
```

### 제약사항

- 건물관리번호는 중복될 수 없다.
- 임시 등록한 사용자를 기록한다.
- 관리자가 승인하기 전에는 다른 사용자의 검색 결과에 노출하지 않는다.

---

# 8. 관리자 아파트 API

모든 API는 `ADMIN` 권한이 필요하다.

## 8.1 관리자 아파트 목록

```http
GET /api/admin/apartments?status={status}&keyword={keyword}&page={page}&size={size}
```

### Status

```text
PENDING
APPROVED
REJECTED
```

---

## 8.2 관리자 아파트 상세 조회

```http
GET /api/admin/apartments/{apartmentId}
```

---

## 8.3 관리자 아파트 직접 등록

```http
POST /api/admin/apartments
```

### Request

```json
{
  "name": "관리자등록아파트",
  "roadAddress": "서울특별시 테스트구 관리로 10",
  "jibunAddress": "서울특별시 테스트구 관리동 100",
  "buildingManagementNumber": "1111111111111111111111111",
  "latitude": 37.123456,
  "longitude": 127.123456
}
```

관리자가 직접 등록한 아파트는 기본적으로 다음 상태를 가진다.

```text
APPROVED
```

---

## 8.4 아파트 수정

```http
PATCH /api/admin/apartments/{apartmentId}
```

---

## 8.5 아파트 승인

```http
PATCH /api/admin/apartments/{apartmentId}/approve
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아파트 등록이 승인되었습니다.",
  "data": {
    "id": 2,
    "status": "APPROVED",
    "approvedAt": "2026-07-18T19:00:00"
  }
}
```

---

## 8.6 아파트 거절

```http
PATCH /api/admin/apartments/{apartmentId}/reject
```

### Request

```json
{
  "reason": "동일한 건물관리번호를 가진 아파트가 이미 존재합니다."
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아파트 등록이 거절되었습니다.",
  "data": {
    "id": 2,
    "status": "REJECTED",
    "rejectionReason": "동일한 건물관리번호를 가진 아파트가 이미 존재합니다."
  }
}
```

---

# 9. 폐기물 카테고리 API

## 9.1 카테고리 목록 조회

```http
GET /api/waste/categories
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "폐기물 카테고리 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "code": "PLASTIC",
      "name": "플라스틱",
      "description": "플라스틱 재질의 재활용 품목",
      "sortOrder": 1
    },
    {
      "id": 2,
      "code": "PAPER",
      "name": "종이류",
      "description": "종이 재질의 재활용 품목",
      "sortOrder": 2
    }
  ]
}
```

---

# 10. 폐기물 품목과 가이드 API

## 10.1 품목 목록 및 검색

```http
GET /api/waste/items?keyword={keyword}&categoryId={categoryId}&page={page}&size={size}
```

### 예시

```http
GET /api/waste/items?keyword=페트병&categoryId=1&page=0&size=20
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "폐기물 품목 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "투명 페트병",
        "imageUrl": "/images/waste/pet-bottle.png",
        "category": {
          "id": 1,
          "code": "PLASTIC",
          "name": "플라스틱"
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

## 10.2 품목 상세 및 가이드 조회

```http
GET /api/waste/items/{wasteItemId}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "폐기물 품목 상세 조회 성공",
  "data": {
    "id": 1,
    "name": "투명 페트병",
    "imageUrl": "/images/waste/pet-bottle.png",
    "category": {
      "id": 1,
      "code": "PLASTIC",
      "name": "플라스틱"
    },
    "guide": {
      "id": 1,
      "summary": "내용물을 비우고 라벨을 제거해 배출합니다.",
      "disposalMethod": "내용물을 비운 후 라벨을 제거하고 찌그러뜨려 배출합니다.",
      "caution": "이물질이 심하게 묻은 경우 일반 종량제 봉투로 배출합니다.",
      "checkItems": [
        {
          "id": 1,
          "content": "내용물을 비웠나요?",
          "sortOrder": 1,
          "required": true
        },
        {
          "id": 2,
          "content": "라벨을 제거했나요?",
          "sortOrder": 2,
          "required": true
        }
      ]
    },
    "schedule": {
      "availableToday": true,
      "nextDisposalDate": "2026-07-21",
      "dayOfWeek": "TUESDAY",
      "startTime": "18:00",
      "endTime": "22:00",
      "alwaysAvailable": false
    }
  }
}
```

사용자가 아파트를 설정하지 않은 경우 `schedule`은 `null`일 수 있다.

---

# 11. 관리자 폐기물 관리 API

모든 API는 `ADMIN` 권한이 필요하다.

## 11.1 카테고리 등록

```http
POST /api/admin/waste/categories
```

## 11.2 카테고리 수정

```http
PATCH /api/admin/waste/categories/{categoryId}
```

## 11.3 카테고리 비활성화

```http
PATCH /api/admin/waste/categories/{categoryId}/deactivate
```

## 11.4 품목 등록

```http
POST /api/admin/waste/items
```

## 11.5 품목 수정

```http
PATCH /api/admin/waste/items/{wasteItemId}
```

## 11.6 품목 비활성화

```http
PATCH /api/admin/waste/items/{wasteItemId}/deactivate
```

## 11.7 가이드 등록 또는 수정

```http
PUT /api/admin/waste/items/{wasteItemId}/guide
```

### Request

```json
{
  "summary": "내용물을 비우고 라벨을 제거해 배출합니다.",
  "disposalMethod": "내용물을 비운 후 라벨을 제거하고 찌그러뜨려 배출합니다.",
  "caution": "이물질이 심하면 종량제 봉투로 배출합니다.",
  "checkItems": [
    {
      "content": "내용물을 비웠나요?",
      "sortOrder": 1,
      "required": true
    },
    {
      "content": "라벨을 제거했나요?",
      "sortOrder": 2,
      "required": true
    }
  ]
}
```

---

# 12. 배출 일정 API

## 12.1 오늘 배출 일정 조회

현재 사용자의 아파트를 기준으로 조회한다.

```http
GET /api/schedules/today
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "오늘 배출 일정 조회 성공",
  "data": {
    "date": "2026-07-18",
    "apartment": {
      "id": 1,
      "name": "스마트아파트"
    },
    "schedules": [
      {
        "id": 1,
        "categoryId": 1,
        "categoryName": "플라스틱",
        "dayOfWeek": "SATURDAY",
        "startTime": "18:00",
        "endTime": "22:00",
        "alwaysAvailable": false
      }
    ]
  }
}
```

---

## 12.2 주간 배출 일정 조회

```http
GET /api/schedules/weekly
```

---

## 12.3 품목의 다음 배출 일정 조회

```http
GET /api/schedules/next?wasteItemId={wasteItemId}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "다음 배출 일정 조회 성공",
  "data": {
    "wasteItemId": 1,
    "wasteItemName": "투명 페트병",
    "availableToday": false,
    "nextDisposalDate": "2026-07-21",
    "dayOfWeek": "TUESDAY",
    "startTime": "18:00",
    "endTime": "22:00"
  }
}
```

---

# 13. 관리자 배출 일정 API

모든 API는 `ADMIN` 권한이 필요하다.

## 13.1 아파트 공식 일정 목록

```http
GET /api/admin/apartments/{apartmentId}/schedules
```

## 13.2 공식 일정 등록

```http
POST /api/admin/apartments/{apartmentId}/schedules
```

### Request

```json
{
  "categoryId": 1,
  "dayOfWeek": "TUESDAY",
  "startTime": "18:00",
  "endTime": "22:00",
  "alwaysAvailable": false
}
```

## 13.3 공식 일정 수정

```http
PATCH /api/admin/schedules/{scheduleId}
```

## 13.4 공식 일정 비활성화

```http
PATCH /api/admin/schedules/{scheduleId}/deactivate
```

---

# 14. 주민 일정 제안과 투표 API

## 14.1 제안 목록 조회

현재 사용자와 같은 아파트의 제안만 조회한다.

```http
GET /api/schedule-proposals?status={status}&page={page}&size={size}
```

### Status

```text
PENDING
APPROVED
REJECTED
CANCELLED
```

---

## 14.2 일정 제안 등록

```http
POST /api/schedule-proposals
```

### Request

```json
{
  "categoryId": 1,
  "proposedDayOfWeek": "WEDNESDAY",
  "proposedStartTime": "18:00",
  "proposedEndTime": "22:00",
  "alwaysAvailable": false
}
```

---

## 14.3 내가 등록한 제안 취소

```http
PATCH /api/schedule-proposals/{proposalId}/cancel
```

다음 조건에서만 취소할 수 있다.

```text
제안자 본인
상태가 PENDING
```

---

## 14.4 투표 등록

```http
POST /api/schedule-proposals/{proposalId}/vote
```

### 제약사항

- 같은 아파트 주민만 투표할 수 있다.
- 한 사용자는 같은 제안에 한 번만 투표할 수 있다.
- `PENDING` 상태의 제안에만 투표할 수 있다.

---

## 14.5 투표 취소

```http
DELETE /api/schedule-proposals/{proposalId}/vote
```

---

## 14.6 제안별 투표 집계

```http
GET /api/schedule-proposals/{proposalId}/summary
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "투표 집계 조회 성공",
  "data": {
    "proposalId": 1,
    "voteCount": 15,
    "votedByMe": true,
    "status": "PENDING"
  }
}
```

---

# 15. 관리자 주민 제안 API

## 15.1 관리자 제안 목록

```http
GET /api/admin/schedule-proposals?apartmentId={apartmentId}&status={status}
```

## 15.2 제안 승인

```http
PATCH /api/admin/schedule-proposals/{proposalId}/approve
```

### Request

```json
{
  "applyToOfficialSchedule": true,
  "reviewComment": "주민 투표 결과를 반영했습니다."
}
```

## 15.3 제안 거절

```http
PATCH /api/admin/schedule-proposals/{proposalId}/reject
```

### Request

```json
{
  "reviewComment": "기존 수거 업체 일정과 맞지 않습니다."
}
```

---

# 16. 주소 검색 API

## 16.1 카카오 주소 검색

```http
GET /api/addresses/search?query={query}
```

### 예시

```http
GET /api/addresses/search?query=서울특별시 강남구
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "주소 검색 성공",
  "data": [
    {
      "roadAddress": "서울특별시 강남구 테헤란로 1",
      "jibunAddress": "서울특별시 강남구 역삼동 1",
      "buildingName": "테스트아파트",
      "buildingManagementNumber": "1234567890123456789012345",
      "latitude": 37.123456,
      "longitude": 127.123456
    }
  ]
}
```

---

# 17. 지자체 가이드 API

## 17.1 현재 위치 기준 지자체 가이드 조회

```http
GET /api/municipal-guides/current?latitude={latitude}&longitude={longitude}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "지자체 분리배출 기준 조회 성공",
  "data": {
    "municipalityCode": "11680",
    "municipalityName": "서울특별시 강남구",
    "lastUpdatedAt": "2026-07-18T10:00:00",
    "cached": false,
    "guides": []
  }
}
```

외부 API 장애 시 마지막 저장 데이터를 반환한다.

```json
{
  "cached": true
}
```

---

# 18. 이미지 분석 기록 API

## 18.1 이미지 업로드 및 분석 기록 생성

```http
POST /api/image-logs
Content-Type: multipart/form-data
```

### Form Data

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `image` | File | 필수 | 촬영 또는 갤러리 이미지 |
| `deviceItemId` | Long | 선택 | TFLite가 예측한 품목 |
| `deviceConfidence` | Decimal | 선택 | TFLite 신뢰도 |
| `deviceModelVersion` | String | 선택 | TFLite 모델 버전 |

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "이미지 분석 기록이 저장되었습니다.",
  "data": {
    "imageLogId": 1,
    "imageUrl": "/uploads/images/uuid.jpg",
    "serverAnalysisRequired": false
  }
}
```

---

## 18.2 내 최근 인식 기록 조회

```http
GET /api/image-logs/me?page={page}&size={size}
```

---

## 18.3 사용자 결과 수정

```http
PATCH /api/image-logs/{imageLogId}/correction
```

### Request

```json
{
  "correctedItemId": 2
}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "분석 결과가 수정되었습니다.",
  "data": {
    "imageLogId": 1,
    "originalItemId": 1,
    "correctedItemId": 2,
    "reviewStatus": "PENDING"
  }
}
```

---

# 19. 가짜 AI 분석 API

실제 모델 연동 전에 Flutter 전체 흐름을 테스트하기 위한 API이다.

## 19.1 가짜 분석 실행

```http
POST /api/ai/mock-analyze
```

### Request

```json
{
  "scenario": "HIGH_CONFIDENCE"
}
```

### Scenario

```text
HIGH_CONFIDENCE
LOW_CONFIDENCE
NOT_RECOGNIZED
```

### 높은 신뢰도 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "AI 분석 성공",
  "data": {
    "wasteItemId": 1,
    "wasteItemName": "투명 페트병",
    "confidence": 0.95,
    "serverAnalysisRequired": false
  }
}
```

### 낮은 신뢰도 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "서버 재분석이 필요합니다.",
  "data": {
    "wasteItemId": 1,
    "wasteItemName": "투명 페트병",
    "confidence": 0.43,
    "serverAnalysisRequired": true
  }
}
```

---

# 20. 서버 AI 분석 API

## 20.1 Python YOLO 서버 재분석

Flutter가 Python 서버를 직접 호출하지 않는다.

```text
Flutter
→ Spring Boot
→ Python AI Server
→ Spring Boot
→ Flutter
```

```http
POST /api/ai/server-analyze/{imageLogId}
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "서버 AI 분석 성공",
  "data": {
    "imageLogId": 1,
    "wasteItemId": 1,
    "wasteItemName": "투명 페트병",
    "confidence": 0.91,
    "modelVersion": "yolo-v1"
  }
}
```

---

# 21. 관리자 AI 검수 API

## 21.1 검수 대기 목록

```http
GET /api/admin/image-logs?reviewStatus=PENDING&page=0&size=20
```

## 21.2 이미지 분석 상세

```http
GET /api/admin/image-logs/{imageLogId}
```

## 21.3 학습 후보 승인

```http
PATCH /api/admin/image-logs/{imageLogId}/approve
```

## 21.4 학습 후보 거절

```http
PATCH /api/admin/image-logs/{imageLogId}/reject
```

### Request

```json
{
  "reason": "이미지가 흐려 품목을 확인할 수 없습니다."
}
```

---

# 22. 알림 API

## 22.1 내 알림 목록

```http
GET /api/notifications?page={page}&size={size}
```

## 22.2 읽지 않은 알림 개수

```http
GET /api/notifications/unread-count
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "읽지 않은 알림 개수 조회 성공",
  "data": {
    "count": 3
  }
}
```

## 22.3 알림 읽음 처리

```http
PATCH /api/notifications/{notificationId}/read
```

## 22.4 전체 알림 읽음 처리

```http
PATCH /api/notifications/read-all
```

---

# 23. 푸시 토큰 API

## 23.1 푸시 토큰 등록 또는 갱신

```http
POST /api/push-tokens
```

### Request

```json
{
  "token": "firebase-device-token",
  "platform": "ANDROID"
}
```

## 23.2 푸시 토큰 비활성화

로그아웃 또는 기기 알림 해제 시 사용한다.

```http
DELETE /api/push-tokens
```

### Request

```json
{
  "token": "firebase-device-token"
}
```

---

# 24. 관리자 대시보드 API

## 24.1 대시보드 요약

```http
GET /api/admin/dashboard
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "관리자 대시보드 조회 성공",
  "data": {
    "pendingApartmentCount": 3,
    "pendingProposalCount": 5,
    "pendingImageReviewCount": 12,
    "todayImageAnalysisCount": 40,
    "activeUserCount": 120
  }
}
```

---

# 25. Flutter용 응답 DTO

Flutter 화면은 중첩 요청을 줄이기 위해 화면에 필요한 데이터를 조합해서 받는다.

## 25.1 홈 응답

```http
GET /api/home
```

### Response

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "홈 정보 조회 성공",
  "data": {
    "user": {
      "nickname": "테스트사용자"
    },
    "apartment": {
      "id": 1,
      "name": "스마트아파트"
    },
    "todaySchedules": [],
    "nextSchedule": null,
    "recentImageLogs": [],
    "unreadNotificationCount": 3
  }
}
```

`/api/home`은 Flutter 홈 화면 전용 조합 API로 사용한다.

---

# 26. 관리자 웹용 응답 DTO

관리자 웹 목록 응답에는 다음 정보가 포함될 수 있다.

```text
페이지 정보
검색 조건
상태별 개수
등록자 정보
승인자 또는 검수자 정보
생성일
수정일
```

관리자 DTO는 Flutter DTO와 분리한다.

예시:

```text
ApartmentResponse
AdminApartmentResponse

ImageLogResponse
AdminImageLogResponse
```

관리자 응답에는 운영용 정보가 포함되지만 사용자 응답에는 노출하지 않는다.

```text
registeredBy
reviewedBy
rejectionReason
internalMemo
modelVersion
createdAt
updatedAt
```

---

# 27. 오류 코드 규칙

## 27.1 공통 및 인증

| 접두사 | 영역 |
|---|---|
| `COMMON` | 공통 입력 및 요청 |
| `AUTH` | 인증 및 접근 권한 |
| `TOKEN` | JWT 및 Refresh Token |
| `SERVER` | 서버 내부 오류 |

## 27.2 EchoSnap 도메인

| 접두사 | 영역 |
|---|---|
| `USER` | 사용자 |
| `APARTMENT` | 아파트 |
| `WASTE` | 폐기물 품목 및 가이드 |
| `SCHEDULE` | 공식 배출 일정 |
| `VOTE` | 주민 제안 및 투표 |
| `ADDRESS` | 주소 검색 API |
| `MUNICIPAL` | 지자체 공공데이터 |
| `IMAGE` | 이미지 업로드 및 기록 |
| `AI` | AI 분석 |
| `NOTIFICATION` | 알림 |
| `PUSH` | 푸시 토큰 |

### 예시

```text
APARTMENT_001: 아파트를 찾을 수 없습니다.
APARTMENT_002: 이미 등록된 아파트입니다.
APARTMENT_003: 승인된 아파트만 선택할 수 있습니다.

WASTE_001: 폐기물 품목을 찾을 수 없습니다.
WASTE_002: 이미 등록된 폐기물 품목입니다.

SCHEDULE_001: 배출 일정을 찾을 수 없습니다.
SCHEDULE_002: 동일한 배출 일정이 이미 존재합니다.

VOTE_001: 일정 제안을 찾을 수 없습니다.
VOTE_002: 이미 투표한 제안입니다.
VOTE_003: 같은 아파트 주민만 투표할 수 있습니다.

IMAGE_001: 지원하지 않는 이미지 형식입니다.
IMAGE_002: 이미지 용량 제한을 초과했습니다.

AI_001: AI 서버 호출에 실패했습니다.
AI_002: AI 분석 시간이 초과되었습니다.
```

---

# 28. 구현 상태

## 현재 구현 완료

```text
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/reissue
GET  /api/users/me
```

## 다음 구현 순서

```text
1. 사용자 정보 확장 API
2. 아파트 API
3. 폐기물 카테고리와 품목 API
4. 분리배출 가이드 API
5. 아파트별 배출 일정 API
6. 주민 일정 제안과 투표 API
7. 주소 검색 API
8. 지자체 공공데이터 API
9. 이미지 업로드와 인식 기록 API
10. 가짜 AI API
11. Python AI 서버 연동
12. 알림과 푸시 토큰 API
13. 관리자 API
```

---

# 29. 1차 API 명세 확정 사항

- 사용자 앱은 Flutter로 구현한다.
- React는 관리자 웹 전용으로 사용한다.
- 모든 플랫폼은 Spring Boot API를 사용한다.
- 일반 사용자 API와 관리자 API를 경로로 분리한다.
- API 성공 및 실패 응답은 `ApiResponse<T>`로 통일한다.
- 목록 응답은 공통 페이지 응답 구조를 사용한다.
- 사용자용 DTO와 관리자용 DTO를 분리한다.
- 실제 품목은 `WasteItem`, 넓은 분류는 `WasteCategory`로 관리한다.
- 주민 일정 제안과 투표 기록을 분리한다.
- Flutter는 Python AI 서버를 직접 호출하지 않는다.
- 외부 API와 Python AI 서버 오류는 Spring Boot에서 변환한다.

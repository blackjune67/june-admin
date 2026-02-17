# RBAC 권한 부여 페이지 API 스펙

이 문서는 프론트엔드에서 권한 부여 페이지를 구현할 때 필요한 요청/응답 규격을 정리한 문서입니다.

## 1. 공통 규칙

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- 인증 헤더: `Authorization: Bearer {accessToken}`
- 성공 응답 포맷:

```json
{
  "success": true,
  "data": {},
  "message": "성공 메시지",
  "timestamp": "2026-02-17T16:40:00.000"
}
```

- 실패 응답 포맷(ProblemDetail, RFC 9457):

```json
{
  "type": "https://api.dh-admin.dev/problems/EFC9403",
  "title": "Forbidden",
  "status": 403,
  "detail": "접근 권한이 없습니다.",
  "instance": "/api/v1/roles/1/permissions",
  "errorCode": "EFC9403",
  "timestamp": "2026-02-17T16:40:00.000+09:00"
}
```

---

## 2. 역할에 권한 부여 (Role-Permission)

### 2.1 역할 목록 조회

- Method: `GET`
- URL: `/api/v1/roles`
- 필요 권한: `role:read`

### 2.2 권한 목록 조회

- Method: `GET`
- URL: `/api/v1/permissions`
- 필요 권한: `permission:read`
- 응답은 `resource` 기준 그룹 구조

### 2.3 특정 역할에 권한 할당

- Method: `PUT`
- URL: `/api/v1/roles/{roleId}/permissions`
- 필요 권한: `role:update`

요청 Body:

```json
{
  "permissionIds": [1, 2, 3]
}
```

요청값 설명:

- `permissionIds`: `Long` 배열
- 빈 배열 전송 시 해당 역할의 권한이 모두 제거됩니다.

성공 응답 예시:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "ADMIN",
    "name": "관리자",
    "description": "일반 관리자",
    "isActive": true,
    "isSystem": true,
    "permissions": [
      {
        "id": 2,
        "resource": "user",
        "action": "read",
        "name": "사용자 조회",
        "description": null,
        "authority": "user:read"
      }
    ]
  },
  "message": "권한 할당 성공",
  "timestamp": "2026-02-17T16:40:00.000"
}
```

주의:

- 이 API는 부분 추가가 아니라 전체 교체 방식입니다.
- 프론트는 최종 선택된 전체 `permissionIds`를 보내야 합니다.

---

## 3. 사용자에 역할 부여 (User-Role)

### 3.1 사용자 목록 조회 (역할 관리용)

- Method: `GET`
- URL: `/api/v1/users`
- 필요 권한: `user:read`

Query Params:

- `keyword` (optional): 이름/이메일 검색어
- `roleCode` (optional): 역할 코드 필터 (예: `ADMIN`, `SUPER_ADMIN`)
- `isActive` (optional): 활성 여부 (`true`/`false`)
- `page` (optional, default `0`): 페이지 번호
- `size` (optional, default `20`, max `100`): 페이지 크기

설계 이유 (Query String 사용):

- `GET` 조회 API의 필터/페이지 파라미터는 query string으로 표현하는 것이 REST 관례에 맞습니다.
- URL만으로 조회 조건을 재현할 수 있어 디버깅/공유/운영 로그 추적이 쉽습니다.
- `page`, `size`, `keyword` 같은 조회 조건은 캐시/프록시 계층에서도 다루기 수월합니다.

확장 가이드:

- 필터 조건이 크게 늘어나 query string이 과도하게 길어지면, 별도 검색 API(예: `POST /api/v1/users/search`)로 분리하고 JSON Body를 사용하는 것을 권장합니다.

요청 예시:

```bash
GET /api/v1/users?keyword=kim&roleCode=ADMIN&isActive=true&page=0&size=20
```

성공 응답 예시:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "email": "kim@example.com",
        "name": "김관리",
        "isActive": true,
        "roles": [
          {
            "id": 1,
            "code": "ADMIN",
            "name": "관리자"
          }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": null,
  "timestamp": "2026-02-17T16:40:00.000"
}
```

### 3.2 사용자의 현재 역할 조회

- Method: `GET`
- URL: `/api/v1/users/{userId}/roles`
- 필요 권한: `user:read`
- 참고: `userId`는 사용자 목록 API 또는 별도 관리 화면의 사용자 식별값을 사용해야 합니다.

### 3.3 사용자 역할 할당

- Method: `PUT`
- URL: `/api/v1/users/{userId}/roles`
- 필요 권한: `user:update`

요청 Body:

```json
{
  "roleIds": [1, 3]
}
```

요청값 설명:

- `roleIds`: `Long` 배열
- 빈 배열 전송 시 해당 사용자의 역할이 모두 제거됩니다.

성공 응답 예시:

```json
{
  "success": true,
  "data": null,
  "message": "역할 할당 성공",
  "timestamp": "2026-02-17T16:40:00.000"
}
```

주의:

- 이 API도 전체 교체 방식입니다.
- 프론트는 체크된 최종 역할 전체 `roleIds`를 보내야 합니다.

---

## 4. 권한 부여 페이지에서 프론트가 보내야 하는 핵심 값

### 역할 권한 할당 시

```json
{
  "permissionIds": [Long, Long, ...]
}
```

### 사용자 역할 할당 시

```json
{
  "roleIds": [Long, Long, ...]
}
```

---

## 5. 추천 API 호출 순서 (화면 로딩 기준)

1. `GET /api/v1/roles` (역할 목록)
2. `GET /api/v1/permissions` (권한 목록)
3. `GET /api/v1/users` (사용자 목록)
4. 역할 선택 후 `PUT /api/v1/roles/{roleId}/permissions`
5. 사용자 선택 후 `GET /api/v1/users/{userId}/roles`
6. 저장 시 `PUT /api/v1/users/{userId}/roles`

---

## 6. 에러 코드 참고

- `EFC9457`: 인증 실패(토큰 만료/누락 등)
- `EFC9403`: 인가 실패(권한 없음)
- `EFC9404`: 대상 없음(역할/사용자/권한 ID)
- `EFC9409`: 중복 데이터
- `EFC9500`: 서버 내부 오류

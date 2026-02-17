# 동적 RBAC 시스템 API 스펙

## 목차
1. [개요](#개요)
2. [공통 규약](#공통-규약)
3. [역할 관리 API](#역할-관리-api)
4. [권한 관리 API](#권한-관리-api)
5. [메뉴 관리 API](#메뉴-관리-api)
6. [사용자-역할 관리 API](#사용자-역할-관리-api)
7. [인증 정보 조회 API](#인증-정보-조회-api)
8. [에러 처리](#에러-처리)

---

## 개요

### API 기본 정보
- **Base URL**: `http://localhost:8080/api/v1`
- **Content-Type**: `application/json`
- **인증**: JWT Bearer Token (Authorization header)
- **응답 형식**: `ApiResponse<T>` Wrapper

### 버전 관리
- **Current Version**: v1
- **Deprecation Policy**: 새 버전은 최소 2개의 마이너 릴리스 후 폐기

---

## 공통 규약

### 응답 포맷

모든 API 응답은 다음 형식을 따릅니다:

```json
{
  "success": true,
  "data": {...},
  "message": "요청 처리 완료",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### ApiResponse<T> Kotlin 정의
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

### 페이지 처리

리스트 응답에는 페이지 정보가 포함됩니다:

```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 50,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### PagedResponse<T> Kotlin 정의
```kotlin
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
```

### 요청 헤더

```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### 응답 상태 코드

| 상태 코드 | 의미 |
|----------|------|
| 200 | OK - 성공 |
| 201 | Created - 생성 성공 |
| 204 | No Content - 성공 (응답 본문 없음) |
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 부족 |
| 404 | Not Found - 리소스 없음 |
| 409 | Conflict - 중복된 리소스 |
| 422 | Unprocessable Entity - 검증 실패 |
| 500 | Internal Server Error - 서버 오류 |

---

## 역할 관리 API

### 1. 역할 목록 조회

**Endpoint**: `GET /roles`

**설명**: 모든 역할을 페이지 단위로 조회합니다.

**권한**: `roles:READ`

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | int | N | 페이지 번호 (기본값: 0) |
| `size` | int | N | 페이지 크기 (기본값: 10, 최대: 100) |
| `sort` | string | N | 정렬 기준 (예: `name,asc`) |
| `search` | string | N | 역할명 또는 코드 검색 |
| `isSystem` | boolean | N | 시스템 역할만 조회 |

**요청 예제**:
```bash
GET /api/v1/roles?page=0&size=20&search=admin&sort=name,asc
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "ROLE_SUPER_ADMIN",
        "name": "슈퍼 관리자",
        "description": "모든 권한을 가진 관리자",
        "isSystem": true,
        "isEnabled": true,
        "permissionCount": 45,
        "createdAt": "2026-01-15T08:00:00Z",
        "updatedAt": "2026-02-10T14:30:00Z"
      },
      {
        "id": 2,
        "code": "ROLE_ADMIN",
        "name": "관리자",
        "description": "시스템 관리 권한",
        "isSystem": true,
        "isEnabled": true,
        "permissionCount": 30,
        "createdAt": "2026-01-15T08:00:00Z",
        "updatedAt": "2026-02-10T14:30:00Z"
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### RoleListResponse Kotlin 정의
```kotlin
data class RoleListResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val permissionCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)
```

---

### 2. 특정 역할 조회

**Endpoint**: `GET /roles/{id}`

**설명**: 특정 역할의 상세 정보와 권한 목록을 조회합니다.

**권한**: `roles:READ`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 역할 ID |

**요청 예제**:
```bash
GET /api/v1/roles/1
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "ROLE_SUPER_ADMIN",
    "name": "슈퍼 관리자",
    "description": "모든 권한을 가진 관리자",
    "isSystem": true,
    "isEnabled": true,
    "createdAt": "2026-01-15T08:00:00Z",
    "updatedAt": "2026-02-10T14:30:00Z",
    "permissions": [
      {
        "id": 1,
        "resource": "users",
        "action": "CREATE",
        "description": "사용자 생성"
      },
      {
        "id": 2,
        "resource": "users",
        "action": "READ",
        "description": "사용자 조회"
      },
      {
        "id": 3,
        "resource": "users",
        "action": "UPDATE",
        "description": "사용자 수정"
      }
    ],
    "userCount": 5
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### RoleDetailResponse Kotlin 정의
```kotlin
data class RoleDetailResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val permissions: List<PermissionResponse>,
    val userCount: Int
)

data class PermissionResponse(
    val id: Long,
    val resource: String,
    val action: String,
    val description: String
)
```

**에러 응답 (404 Not Found)**:
```json
{
  "success": false,
  "data": null,
  "message": "역할을 찾을 수 없습니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 3. 역할 생성

**Endpoint**: `POST /roles`

**설명**: 새로운 역할을 생성합니다.

**권한**: `roles:CREATE`

**Request Body**:
```json
{
  "code": "ROLE_MODERATOR",
  "name": "중재자",
  "description": "커뮤니티 중재 권한",
  "permissionIds": [2, 5, 7]
}
```

#### CreateRoleRequest Kotlin 정의
```kotlin
data class CreateRoleRequest(
    @NotBlank(message = "역할 코드는 필수입니다")
    @Size(min = 5, max = 100, message = "역할 코드는 5~100자여야 합니다")
    val code: String,

    @NotBlank(message = "역할명은 필수입니다")
    @Size(min = 2, max = 255, message = "역할명은 2~255자여야 합니다")
    val name: String,

    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    val description: String?,

    @NotEmpty(message = "최소 1개의 권한을 선택해야 합니다")
    val permissionIds: List<Long>
)
```

**요청 예제**:
```bash
POST /api/v1/roles
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "code": "ROLE_MODERATOR",
  "name": "중재자",
  "description": "커뮤니티 중재 권한",
  "permissionIds": [2, 5, 7]
}
```

**응답 (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": 10,
    "code": "ROLE_MODERATOR",
    "name": "중재자",
    "description": "커뮤니티 중재 권한",
    "isSystem": false,
    "isEnabled": true,
    "createdAt": "2026-02-17T10:30:45Z",
    "updatedAt": null
  },
  "message": "역할이 생성되었습니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

**에러 응답 (409 Conflict - 중복된 코드)**:
```json
{
  "success": false,
  "data": null,
  "message": "이미 존재하는 역할 코드입니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

**에러 응답 (422 Unprocessable Entity - 검증 실패)**:
```json
{
  "success": false,
  "data": {
    "errors": [
      {
        "field": "code",
        "message": "역할 코드는 5~100자여야 합니다"
      },
      {
        "field": "permissionIds",
        "message": "최소 1개의 권한을 선택해야 합니다"
      }
    ]
  },
  "message": "입력 값 검증에 실패했습니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 4. 역할 수정

**Endpoint**: `PUT /roles/{id}`

**설명**: 기존 역할의 정보를 수정합니다. 시스템 역할은 수정할 수 없습니다.

**권한**: `roles:UPDATE`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 역할 ID |

**Request Body**:
```json
{
  "name": "중재자 (업데이트됨)",
  "description": "커뮤니티 중재 권한 - 수정됨",
  "isEnabled": true
}
```

#### UpdateRoleRequest Kotlin 정의
```kotlin
data class UpdateRoleRequest(
    @Size(min = 2, max = 255, message = "역할명은 2~255자여야 합니다")
    val name: String?,

    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    val description: String?,

    val isEnabled: Boolean?
)
```

**요청 예제**:
```bash
PUT /api/v1/roles/10
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "중재자 (업데이트됨)",
  "description": "커뮤니티 중재 권한 - 수정됨",
  "isEnabled": true
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 10,
    "code": "ROLE_MODERATOR",
    "name": "중재자 (업데이트됨)",
    "description": "커뮤니티 중재 권한 - 수정됨",
    "isSystem": false,
    "isEnabled": true,
    "createdAt": "2026-02-17T10:30:45Z",
    "updatedAt": "2026-02-17T10:35:20Z"
  },
  "message": "역할이 수정되었습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

**에러 응답 (403 Forbidden - 시스템 역할 수정 시도)**:
```json
{
  "success": false,
  "data": null,
  "message": "시스템 역할은 수정할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

### 5. 역할 삭제

**Endpoint**: `DELETE /roles/{id}`

**설명**: 역할을 삭제합니다. 시스템 역할은 삭제할 수 없습니다.

**권한**: `roles:DELETE`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 역할 ID |

**요청 예제**:
```bash
DELETE /api/v1/roles/10
Authorization: Bearer {jwt_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

**에러 응답 (403 Forbidden - 시스템 역할 삭제 시도)**:
```json
{
  "success": false,
  "data": null,
  "message": "시스템 역할은 삭제할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

**에러 응답 (409 Conflict - 사용자가 있는 역할 삭제 시도)**:
```json
{
  "success": false,
  "data": {
    "roleId": 10,
    "assignedUserCount": 5
  },
  "message": "이 역할에 할당된 사용자가 있어 삭제할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

### 6. 역할에 권한 할당

**Endpoint**: `PUT /roles/{id}/permissions`

**설명**: 역할에 권한을 할당하거나 제거합니다.

**권한**: `roles:UPDATE`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 역할 ID |

**Request Body**:
```json
{
  "permissionIds": [1, 2, 3, 5, 7],
  "action": "REPLACE"
}
```

#### AssignPermissionsRequest Kotlin 정의
```kotlin
data class AssignPermissionsRequest(
    @NotEmpty(message = "최소 1개의 권한을 선택해야 합니다")
    val permissionIds: List<Long>,

    @NotNull
    val action: PermissionAction = PermissionAction.REPLACE
)

enum class PermissionAction {
    ADD,      // 기존 권한에 추가
    REMOVE,   // 기존 권한에서 제거
    REPLACE   // 기존 권한 모두 대체
}
```

**요청 예제**:
```bash
PUT /api/v1/roles/10/permissions
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "permissionIds": [1, 2, 3, 5, 7],
  "action": "REPLACE"
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 10,
    "code": "ROLE_MODERATOR",
    "name": "중재자",
    "permissions": [
      {
        "id": 1,
        "resource": "users",
        "action": "READ"
      },
      {
        "id": 2,
        "resource": "users",
        "action": "UPDATE"
      },
      {
        "id": 3,
        "resource": "posts",
        "action": "DELETE"
      }
    ]
  },
  "message": "역할에 권한이 할당되었습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

## 권한 관리 API

### 1. 권한 목록 조회

**Endpoint**: `GET /permissions`

**설명**: 모든 권한을 페이지 단위로 조회합니다.

**권한**: `permissions:READ`

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | int | N | 페이지 번호 (기본값: 0) |
| `size` | int | N | 페이지 크기 (기본값: 10) |
| `resource` | string | N | 리소스 필터 |
| `action` | string | N | 액션 필터 |

**요청 예제**:
```bash
GET /api/v1/permissions?page=0&size=50&resource=users
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "resource": "users",
        "action": "CREATE",
        "description": "사용자 생성",
        "createdAt": "2026-01-15T08:00:00Z"
      },
      {
        "id": 2,
        "resource": "users",
        "action": "READ",
        "description": "사용자 조회",
        "createdAt": "2026-01-15T08:00:00Z"
      }
    ],
    "totalElements": 45,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 50,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 2. 리소스별 권한 조회

**Endpoint**: `GET /permissions/resources`

**설명**: 리소스별로 그룹화된 권한을 조회합니다.

**권한**: `permissions:READ`

**요청 예제**:
```bash
GET /api/v1/permissions/resources
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "id": 1,
        "resource": "users",
        "action": "CREATE",
        "description": "사용자 생성"
      },
      {
        "id": 2,
        "resource": "users",
        "action": "READ",
        "description": "사용자 조회"
      }
    ],
    "roles": [
      {
        "id": 7,
        "resource": "roles",
        "action": "CREATE",
        "description": "역할 생성"
      }
    ],
    "menus": [
      {
        "id": 15,
        "resource": "menus",
        "action": "READ",
        "description": "메뉴 조회"
      }
    ]
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 3. 권한 생성

**Endpoint**: `POST /permissions`

**설명**: 새로운 권한을 생성합니다.

**권한**: `permissions:CREATE`

**Request Body**:
```json
{
  "resource": "reports",
  "action": "EXPORT",
  "description": "보고서 내보내기"
}
```

#### CreatePermissionRequest Kotlin 정의
```kotlin
data class CreatePermissionRequest(
    @NotBlank(message = "리소스는 필수입니다")
    @Size(min = 2, max = 100)
    val resource: String,

    @NotBlank(message = "액션은 필수입니다")
    @Size(min = 2, max = 100)
    val action: String,

    @NotBlank(message = "설명은 필수입니다")
    @Size(min = 2, max = 255)
    val description: String
)
```

**요청 예제**:
```bash
POST /api/v1/permissions
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "resource": "reports",
  "action": "EXPORT",
  "description": "보고서 내보내기"
}
```

**응답 (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": 46,
    "resource": "reports",
    "action": "EXPORT",
    "description": "보고서 내보내기",
    "createdAt": "2026-02-17T10:30:45Z"
  },
  "message": "권한이 생성되었습니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

**에러 응답 (409 Conflict - 중복된 권한)**:
```json
{
  "success": false,
  "data": null,
  "message": "이미 존재하는 권한입니다 (리소스: reports, 액션: EXPORT)",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 4. 권한 수정

**Endpoint**: `PUT /permissions/{id}`

**설명**: 권한의 설명을 수정합니다.

**권한**: `permissions:UPDATE`

**Request Body**:
```json
{
  "description": "보고서 Excel 형식 내보내기"
}
```

#### UpdatePermissionRequest Kotlin 정의
```kotlin
data class UpdatePermissionRequest(
    @Size(min = 2, max = 255)
    val description: String?
)
```

**요청 예제**:
```bash
PUT /api/v1/permissions/46
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "description": "보고서 Excel 형식 내보내기"
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 46,
    "resource": "reports",
    "action": "EXPORT",
    "description": "보고서 Excel 형식 내보내기",
    "createdAt": "2026-02-17T10:30:45Z"
  },
  "message": "권한이 수정되었습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

### 5. 권한 삭제

**Endpoint**: `DELETE /permissions/{id}`

**설명**: 권한을 삭제합니다.

**권한**: `permissions:DELETE`

**요청 예제**:
```bash
DELETE /api/v1/permissions/46
Authorization: Bearer {jwt_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

**에러 응답 (409 Conflict - 역할이 있는 권한 삭제 시도)**:
```json
{
  "success": false,
  "data": {
    "permissionId": 46,
    "assignedRoleCount": 3
  },
  "message": "이 권한이 할당된 역할이 있어 삭제할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

## 메뉴 관리 API

### 1. 메뉴 트리 조회

**Endpoint**: `GET /menus`

**설명**: 계층적 메뉴 트리를 조회합니다.

**권한**: `menus:READ`

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roleId` | long | N | 특정 역할의 권한으로 필터링 |
| `includeHidden` | boolean | N | 숨겨진 메뉴 포함 여부 |

**요청 예제**:
```bash
GET /api/v1/menus?roleId=2&includeHidden=false
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "관리",
      "path": "/admin",
      "icon": "shield",
      "displayOrder": 1,
      "requiredPermission": null,
      "children": [
        {
          "id": 2,
          "name": "사용자 관리",
          "path": "/admin/users",
          "icon": "users",
          "displayOrder": 1,
          "requiredPermission": "users:READ",
          "children": []
        },
        {
          "id": 3,
          "name": "역할 관리",
          "path": "/admin/roles",
          "icon": "shield-alt",
          "displayOrder": 2,
          "requiredPermission": "roles:READ",
          "children": []
        }
      ]
    },
    {
      "id": 5,
      "name": "보고서",
      "path": "/reports",
      "icon": "chart-bar",
      "displayOrder": 2,
      "requiredPermission": "reports:READ",
      "children": [
        {
          "id": 6,
          "name": "판매 보고서",
          "path": "/reports/sales",
          "icon": null,
          "displayOrder": 1,
          "requiredPermission": "reports:READ",
          "children": []
        }
      ]
    }
  ],
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### MenuTreeResponse Kotlin 정의
```kotlin
data class MenuTreeResponse(
    val id: Long,
    val name: String,
    val path: String,
    val icon: String?,
    val displayOrder: Int,
    val requiredPermission: String?,
    val children: List<MenuTreeResponse>
)
```

---

### 2. 특정 메뉴 조회

**Endpoint**: `GET /menus/{id}`

**설명**: 특정 메뉴의 상세 정보를 조회합니다.

**권한**: `menus:READ`

**요청 예제**:
```bash
GET /api/v1/menus/2
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 2,
    "name": "사용자 관리",
    "path": "/admin/users",
    "icon": "users",
    "displayOrder": 1,
    "parentId": 1,
    "requiredPermission": "users:READ",
    "isHidden": false,
    "createdAt": "2026-01-15T08:00:00Z",
    "updatedAt": null
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 3. 메뉴 생성

**Endpoint**: `POST /menus`

**설명**: 새로운 메뉴를 생성합니다.

**권한**: `menus:CREATE`

**Request Body**:
```json
{
  "name": "대시보드",
  "path": "/dashboard",
  "icon": "chart-line",
  "displayOrder": 0,
  "parentId": null,
  "requiredPermission": "dashboard:READ",
  "isHidden": false
}
```

#### CreateMenuRequest Kotlin 정의
```kotlin
data class CreateMenuRequest(
    @NotBlank(message = "메뉴명은 필수입니다")
    @Size(min = 1, max = 255)
    val name: String,

    @NotBlank(message = "메뉴 경로는 필수입니다")
    @Size(min = 1, max = 500)
    @Pattern(regexp = "^/[a-zA-Z0-9/_-]*$", message = "유효하지 않은 경로 형식")
    val path: String,

    @Size(max = 100)
    val icon: String?,

    @Min(0)
    val displayOrder: Int = 0,

    val parentId: Long?,

    @Size(max = 255)
    val requiredPermission: String?,

    val isHidden: Boolean = false
)
```

**요청 예제**:
```bash
POST /api/v1/menus
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "대시보드",
  "path": "/dashboard",
  "icon": "chart-line",
  "displayOrder": 0,
  "parentId": null,
  "requiredPermission": "dashboard:READ",
  "isHidden": false
}
```

**응답 (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "대시보드",
    "path": "/dashboard",
    "icon": "chart-line",
    "displayOrder": 0,
    "parentId": null,
    "requiredPermission": "dashboard:READ",
    "isHidden": false,
    "createdAt": "2026-02-17T10:30:45Z",
    "updatedAt": null
  },
  "message": "메뉴가 생성되었습니다",
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 4. 메뉴 수정

**Endpoint**: `PUT /menus/{id}`

**설명**: 메뉴 정보를 수정합니다.

**권한**: `menus:UPDATE`

**Request Body**:
```json
{
  "name": "메인 대시보드",
  "icon": "dashboard",
  "displayOrder": 0,
  "isHidden": false
}
```

#### UpdateMenuRequest Kotlin 정의
```kotlin
data class UpdateMenuRequest(
    @Size(min = 1, max = 255)
    val name: String?,

    @Size(max = 100)
    val icon: String?,

    @Min(0)
    val displayOrder: Int?,

    val isHidden: Boolean?
)
```

**요청 예제**:
```bash
PUT /api/v1/menus/10
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "메인 대시보드",
  "icon": "dashboard",
  "displayOrder": 0,
  "isHidden": false
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "메인 대시보드",
    "path": "/dashboard",
    "icon": "dashboard",
    "displayOrder": 0,
    "parentId": null,
    "requiredPermission": "dashboard:READ",
    "isHidden": false,
    "createdAt": "2026-02-17T10:30:45Z",
    "updatedAt": "2026-02-17T10:35:20Z"
  },
  "message": "메뉴가 수정되었습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

### 5. 메뉴 삭제

**Endpoint**: `DELETE /menus/{id}`

**설명**: 메뉴를 삭제합니다. 자식 메뉴가 있으면 삭제할 수 없습니다.

**권한**: `menus:DELETE`

**요청 예제**:
```bash
DELETE /api/v1/menus/10
Authorization: Bearer {jwt_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

**에러 응답 (409 Conflict - 자식 메뉴가 있는 경우)**:
```json
{
  "success": false,
  "data": {
    "menuId": 10,
    "childMenuCount": 2
  },
  "message": "이 메뉴 아래에 자식 메뉴가 있어 삭제할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

## 사용자-역할 관리 API

### 1. 사용자의 역할 조회

**Endpoint**: `GET /users/{id}/roles`

**설명**: 사용자에게 할당된 모든 역할을 조회합니다.

**권한**: `users:READ`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 사용자 ID |

**요청 예제**:
```bash
GET /api/v1/users/5/roles
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "userId": 5,
    "username": "john.doe",
    "roles": [
      {
        "id": 1,
        "code": "ROLE_ADMIN",
        "name": "관리자",
        "description": "시스템 관리 권한"
      },
      {
        "id": 2,
        "code": "ROLE_MODERATOR",
        "name": "중재자",
        "description": "커뮤니티 중재 권한"
      }
    ]
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

---

### 2. 사용자 역할 할당/변경

**Endpoint**: `PUT /users/{id}/roles`

**설명**: 사용자에게 역할을 할당하거나 제거합니다.

**권한**: `users:UPDATE`

**Path Parameters**:

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | long | 사용자 ID |

**Request Body**:
```json
{
  "roleIds": [1, 2, 5],
  "action": "REPLACE"
}
```

#### AssignRolesToUserRequest Kotlin 정의
```kotlin
data class AssignRolesToUserRequest(
    @NotEmpty(message = "최소 1개의 역할을 선택해야 합니다")
    val roleIds: List<Long>,

    @NotNull
    val action: RoleAction = RoleAction.REPLACE
)

enum class RoleAction {
    ADD,      // 기존 역할에 추가
    REMOVE,   // 기존 역할에서 제거
    REPLACE   // 기존 역할 모두 대체
}
```

**요청 예제**:
```bash
PUT /api/v1/users/5/roles
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "roleIds": [1, 2, 5],
  "action": "REPLACE"
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "userId": 5,
    "username": "john.doe",
    "roles": [
      {
        "id": 1,
        "code": "ROLE_ADMIN",
        "name": "관리자"
      },
      {
        "id": 2,
        "code": "ROLE_MODERATOR",
        "name": "중재자"
      },
      {
        "id": 5,
        "code": "ROLE_ANALYST",
        "name": "분석가"
      }
    ]
  },
  "message": "사용자 역할이 변경되었습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

**에러 응답 (403 Forbidden - 권한 상승 시도)**:
```json
{
  "success": false,
  "data": {
    "attemptedRole": "ROLE_SUPER_ADMIN"
  },
  "message": "자신의 권한보다 높은 역할을 할당할 수 없습니다",
  "timestamp": "2026-02-17T10:35:20.123Z"
}
```

---

## 인증 정보 조회 API

### /auth/me 확장

**Endpoint**: `GET /auth/me`

**설명**: 현재 인증된 사용자의 정보, 역할, 권한, 메뉴를 모두 조회합니다.

**권한**: 없음 (인증된 사용자만 접근)

**요청 예제**:
```bash
GET /api/v1/auth/me
Authorization: Bearer {jwt_token}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 5,
      "username": "john.doe",
      "email": "john.doe@example.com",
      "createdAt": "2026-01-01T00:00:00Z"
    },
    "roles": [
      {
        "id": 1,
        "code": "ROLE_ADMIN",
        "name": "관리자",
        "description": "시스템 관리 권한"
      }
    ],
    "permissions": [
      {
        "id": 1,
        "resource": "users",
        "action": "CREATE",
        "description": "사용자 생성"
      },
      {
        "id": 2,
        "resource": "users",
        "action": "READ",
        "description": "사용자 조회"
      }
    ],
    "menus": [
      {
        "id": 1,
        "name": "관리",
        "path": "/admin",
        "icon": "shield",
        "displayOrder": 1,
        "children": [
          {
            "id": 2,
            "name": "사용자 관리",
            "path": "/admin/users",
            "icon": "users",
            "displayOrder": 1,
            "children": []
          }
        ]
      }
    ]
  },
  "timestamp": "2026-02-17T10:30:45.123Z"
}
```

#### MyInfoResponse Kotlin 정의
```kotlin
data class MyInfoResponse(
    val user: UserInfoDto,
    val roles: List<RoleBasicDto>,
    val permissions: List<PermissionBasicDto>,
    val menus: List<MenuTreeResponse>
)

data class UserInfoDto(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime
)

data class RoleBasicDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?
)

data class PermissionBasicDto(
    val id: Long,
    val resource: String,
    val action: String,
    val description: String
)
```

---

## 에러 처리

### RFC 7807 ProblemDetail 규약

서버 오류 또는 비즈니스 로직 오류는 RFC 7807 표준을 따릅니다.

#### 기본 에러 응답 형식

```json
{
  "type": "https://api.example.com/errors/validation-error",
  "title": "Validation Error",
  "status": 422,
  "detail": "입력 값 검증에 실패했습니다",
  "instance": "/api/v1/roles",
  "timestamp": "2026-02-17T10:30:45.123Z",
  "errors": [
    {
      "field": "code",
      "message": "역할 코드는 5~100자여야 합니다"
    }
  ]
}
```

#### ProblemDetail Kotlin 정의
```kotlin
data class ProblemDetail(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val errors: List<FieldError>? = null
)

data class FieldError(
    val field: String,
    val message: String
)
```

### 일반적인 에러 응답

#### 400 Bad Request - 잘못된 요청
```json
{
  "type": "https://api.example.com/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "요청 형식이 잘못되었습니다",
  "instance": "/api/v1/roles"
}
```

#### 401 Unauthorized - 인증 필요
```json
{
  "type": "https://api.example.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "유효하지 않거나 만료된 토큰입니다",
  "instance": "/api/v1/roles"
}
```

#### 403 Forbidden - 권한 부족
```json
{
  "type": "https://api.example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "이 작업을 수행할 권한이 없습니다",
  "instance": "/api/v1/roles",
  "requiredPermission": "roles:UPDATE"
}
```

#### 404 Not Found - 리소스 없음
```json
{
  "type": "https://api.example.com/errors/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "요청한 리소스를 찾을 수 없습니다",
  "instance": "/api/v1/roles/999"
}
```

#### 409 Conflict - 충돌
```json
{
  "type": "https://api.example.com/errors/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 존재하는 역할 코드입니다",
  "instance": "/api/v1/roles",
  "conflictField": "code"
}
```

#### 422 Unprocessable Entity - 검증 실패
```json
{
  "type": "https://api.example.com/errors/validation-error",
  "title": "Validation Error",
  "status": 422,
  "detail": "입력 값 검증에 실패했습니다",
  "instance": "/api/v1/roles",
  "errors": [
    {
      "field": "code",
      "message": "역할 코드는 5~100자여야 합니다"
    },
    {
      "field": "permissionIds",
      "message": "최소 1개의 권한을 선택해야 합니다"
    }
  ]
}
```

#### 500 Internal Server Error - 서버 오류
```json
{
  "type": "https://api.example.com/errors/internal-server-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "서버에 오류가 발생했습니다",
  "instance": "/api/v1/roles",
  "traceId": "uuid-trace-id"
}
```

---

## API 사용 예제

### 예제 1: 새로운 역할 생성 및 권한 할당

```bash
# Step 1: 사용 가능한 권한 조회
curl -X GET "http://localhost:8080/api/v1/permissions/resources" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json"

# Step 2: 새로운 역할 생성
curl -X POST "http://localhost:8080/api/v1/roles" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ROLE_CONTENT_MANAGER",
    "name": "콘텐츠 관리자",
    "description": "콘텐츠 관리 권한",
    "permissionIds": [15, 16, 17]
  }'

# Step 3: 사용자에게 역할 할당
curl -X PUT "http://localhost:8080/api/v1/users/5/roles" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "roleIds": [2, 3],
    "action": "REPLACE"
  }'
```

### 예제 2: 사용자 정보 및 권한 조회

```bash
# 현재 사용자의 모든 정보 조회
curl -X GET "http://localhost:8080/api/v1/auth/me" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json"

# 메뉴 트리 조회 (현재 사용자 권한 기반)
curl -X GET "http://localhost:8080/api/v1/menus?includeHidden=false" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json"
```

---

## API 버전 관리 및 폐기 정책

### 버전 호환성

- **현재 안정 버전**: v1
- **지원 기간**: v1 출시 후 최소 24개월
- **폐기 공지**: v1 폐기 6개월 전에 공지

### 하위 호환성

- 기존 응답 필드는 유지됨
- 새로운 선택적 필드 추가 가능
- 필수 필드 변경은 새 버전에서만 허용

---

## 성능 고려사항

### 권장 사항

1. **페이지 크기**: 기본 10, 최대 100으로 제한
2. **권한 캐싱**: 클라이언트는 권한 정보 캐싱 권고
3. **메뉴 캐싱**: 메뉴 트리는 자주 변경되지 않으므로 캐싱 권고
4. **배치 작업**: 많은 사용자에게 역할 할당 시 배치 API 사용 권고

### 레이트 제한

- **일반 API**: 분당 60회 요청
- **쓰기 API (POST, PUT, DELETE)**: 분당 30회 요청

---

## 보안 고려사항

### 권한 검증

- 모든 API 엔드포인트는 `@PreAuthorize` 어노테이션으로 보호됨
- 사용자는 자신의 권한보다 높은 권한을 할당할 수 없음
- 시스템 역할(`is_system=true`)은 변경 불가능

### 토큰 관리

- JWT 토큰에는 역할 코드만 포함
- 권한은 실시간으로 DB에서 조회하여 즉시 반영
- 토큰 갱신 시 새로운 권한을 포함하여 발급

### 감시 로깅

- 모든 권한 변경 사항은 감시 로그에 기록됨
- 수행자, 대상, 변경 사항, 시간이 기록됨


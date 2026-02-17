# 로그인 API 명세 (React 연동용)

이 문서는 `src/main/kotlin/com/dh/admin/interfaces/api/AuthController.kt` 기준의 인증 API 명세입니다.

## 1. 로그인

- Method: `POST`
- URL: `/api/v1/auth/login`
- Auth: 불필요
- Content-Type: `application/json`

### 요청 Body

```json
{
  "email": "admin@example.com",
  "password": "your-password"
}
```

### 요청값 규칙

- `email`: 필수, 이메일 형식
- `password`: 필수, 빈 문자열 불가

### 성공 응답 (`200 OK`)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  },
  "message": "로그인 성공",
  "timestamp": "2026-02-16T23:59:59.123"
}
```

### 실패 응답 예시

에러 응답은 RFC 9457 `ProblemDetail` 형식으로 내려갑니다.

#### 1) 요청값 검증 실패 (`400 Bad Request`)

```json
{
  "type": "https://api.dh-admin.dev/problems/EFC9400",
  "title": "Validation Error",
  "status": 400,
  "detail": "email: 이메일 형식이 올바르지 않습니다., password: 비밀번호는 필수입니다.",
  "instance": "/api/v1/auth/login",
  "errorCode": "EFC9400",
  "timestamp": "2026-02-16T23:59:59.123+09:00"
}
```

#### 2) 인증 실패 (`401 Unauthorized`)

```json
{
  "type": "https://api.dh-admin.dev/problems/EFC9457",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "instance": "/api/v1/auth/login",
  "errorCode": "EFC9457",
  "timestamp": "2026-02-16T23:59:59.123+09:00"
}
```

#### 3) 계정 상태 문제 (`400 Bad Request`)

```json
{
  "type": "https://api.dh-admin.dev/problems/EFC9400",
  "title": "Validation Error",
  "status": 400,
  "detail": "계정이 잠겨있습니다. 관리자에게 문의하세요.",
  "instance": "/api/v1/auth/login",
  "errorCode": "EFC9400",
  "timestamp": "2026-02-16T23:59:59.123+09:00"
}
```

## 2. React에서 실제로 보내야 하는 값

로그인 시 프론트에서 백엔드로 보내야 하는 값은 아래 2개입니다.

- `email` (string)
- `password` (string)

즉, 최소 요청 형태는 아래와 같습니다.

```json
{
  "email": "admin@example.com",
  "password": "plain-password"
}
```

## 3. 로그인 후 프론트 처리 규칙

1. 응답의 `data.accessToken`을 메모리(권장) 또는 저장소에 보관합니다.
2. API 호출 시 HTTP 헤더에 아래처럼 전달합니다.
   - `Authorization: Bearer {accessToken}`
3. Access Token 만료 시 `refreshToken`으로 `/api/v1/auth/refresh`를 호출해 재발급합니다.

## 4. 토큰 재발급 API (권장 같이 구현)

- Method: `POST`
- URL: `/api/v1/auth/refresh`
- Auth: 불필요
- Content-Type: `application/json`

### 요청 Body

```json
{
  "refreshToken": "로그인 시 받은 refreshToken"
}
```

## 5. Axios 예시

```ts
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

export async function login(email: string, password: string) {
  const res = await api.post("/api/v1/auth/login", { email, password });
  return res.data;
}
```

## 6. CORS 참고

현재 서버 CORS 허용 Origin은 `http://localhost:3000` 입니다.

- 파일: `src/main/kotlin/com/dh/admin/infrastructure/security/SecurityConfig.kt`
- React 개발 서버가 `5173` 등 다른 포트면 `allowedOrigins`에 추가해야 합니다.

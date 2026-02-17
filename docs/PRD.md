# PRD: 관리자 백엔드 시스템

## 1. 프로젝트 개요

### 1.1 목적
React 기반 관리자 프론트엔드와 연동되는 백엔드 API 서버를 구축한다. 다양한 보고서 기능과 Spring Security 기반 인증/인가를 핵심으로 하며, DDD 아키텍처와 TDD/SDD 개발 방법론을 적용한다.

### 1.2 핵심 목표
- Spring Security 기반 관리자 로그인/인가 시스템 구현
- 다양한 형태의 보고서 조회/생성/내보내기 기능 제공
- React 프론트엔드와의 안정적인 REST API 통신
- DDD 기반의 확장 가능한 아키텍처 설계

---

## 2. 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Kotlin | 2.2.21 |
| Runtime | JVM (Java) | 24 |
| Framework | Spring Boot | 4.0.2 |
| Build Tool | Gradle (Kotlin DSL) | 9.3.0 |
| Web | Spring MVC (REST API) | - |
| Security | Spring Security | - |
| ORM | Spring Data JPA | - |
| JSON | Jackson Kotlin Module | - |
| Database | PostgreSQL | latest stable |
| Config | application.yaml | - |

### 프론트엔드 연동
- **프레임워크**: React
- **통신 방식**: REST API (JSON)
- **CORS**: 관리자 프론트엔드 도메인 허용

---

## 3. 아키텍처

### 3.1 개발 방법론
- **DDD (Domain-Driven Design)**: 도메인 중심 설계
- **TDD (Test-Driven Development)**: 테스트 선행 개발
- **SDD (Spec-Driven Development)**: 명세(스펙) 우선 정의 후 구현

### 3.2 SDD 프로세스
SDD는 구현 전에 명세를 먼저 작성하고, 명세를 기준으로 개발·검증하는 방법론이다.

1. **Spec 작성**: 기능 요구사항, API 명세(OpenAPI/Swagger), 도메인 모델 정의
2. **Spec 리뷰**: 팀원 간 명세 리뷰 및 프론트엔드와 API 계약 합의
3. **Spec 기반 구현**: 합의된 명세를 기준으로 백엔드/프론트엔드 병렬 개발
4. **Spec 기반 검증**: 명세 대비 구현 일치 여부를 자동화 테스트로 검증

> **TDD와의 관계**: TDD는 코드 수준의 테스트 선행, SDD는 설계 수준의 명세 선행이다. 두 방법론을 함께 적용하여 "명세 → 테스트 → 구현" 순서로 개발한다.

### 3.3 DDD 패키지 구조

```
src/main/kotlin/com/dh/admin/
├── domain/                    # 도메인 계층
│   ├── auth/                  # 인증/인가 도메인
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── exception/
│   ├── report/                # 보고서 도메인
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── exception/
│   └── user/                  # 사용자(관리자) 도메인
│       ├── entity/
│       ├── repository/
│       ├── service/
│       └── exception/
├── application/               # 응용 계층
│   ├── dto/                   # 요청/응답 DTO
│   └── facade/                # 도메인 간 조합 로직
├── infrastructure/            # 인프라 계층
│   ├── config/                # Spring 설정
│   ├── security/              # Security 설정
│   ├── persistence/           # JPA 설정, QueryDSL
│   └── external/              # 외부 서비스 연동
├── interfaces/                # 인터페이스 계층
│   ├── api/                   # REST Controller
│   └── advice/                # 전역 예외 처리
└── common/                    # 공통 유틸리티
    ├── exception/
    ├── response/
    └── util/
```

---

## 4. 코딩 원칙

- **Kotlin 우선**: null-safety, data class, extension functions 등 적극 활용
- **불변성(Immutability)**: `val` 우선 사용, 가변 상태 최소화
- **함수형 스타일**: `map`, `filter`, `let`, `run` 등 활용
- **명확한 네이밍**: 축약어 지양, 의도가 명확한 이름 사용

---

## 5. 핵심 기능 요구사항

### 5.1 인증/인가 (Phase 1 - 최우선)

#### 로그인
- Spring Security 기반 관리자 로그인
- JWT 토큰 발급 (Access Token + Refresh Token)
- 로그인 실패 횟수 제한 및 계정 잠금

#### 권한 관리
- Role 기반 접근 제어 (RBAC)
  - `SUPER_ADMIN`: 전체 권한
  - `ADMIN`: 일반 관리 권한
  - `VIEWER`: 조회 전용 권한
- SecurityConfig에서 엔드포인트별 권한 설정

#### 세션/토큰 관리
- JWT 만료 시 Refresh Token으로 재발급
- 로그아웃 시 토큰 무효화

#### API 엔드포인트
```
POST   /api/v1/auth/login          # 로그인
POST   /api/v1/auth/logout         # 로그아웃
POST   /api/v1/auth/refresh        # 토큰 갱신
GET    /api/v1/auth/me             # 현재 사용자 정보
```

### 5.2 보고서 시스템 (Phase 2)

#### 보고서 유형 (확장 가능한 구조)
- 일간/주간/월간/연간 보고서
- 사용자 활동 보고서
- 매출/통계 보고서
- 커스텀 보고서 (필터 기반)

#### 보고서 기능
- 다양한 필터 조건으로 데이터 조회
- 페이지네이션, 정렬 지원
- 보고서 내보내기 (CSV, Excel, PDF)
- 보고서 템플릿 저장/관리

#### API 엔드포인트
```
GET    /api/v1/reports              # 보고서 목록 조회
GET    /api/v1/reports/{id}         # 보고서 상세 조회
POST   /api/v1/reports              # 보고서 생성
GET    /api/v1/reports/{id}/export  # 보고서 내보내기
GET    /api/v1/reports/templates    # 보고서 템플릿 목록
POST   /api/v1/reports/templates    # 보고서 템플릿 저장
```

### 5.3 관리자 사용자 관리 (Phase 2)

#### API 엔드포인트
```
GET    /api/v1/users                # 관리자 목록 조회
POST   /api/v1/users                # 관리자 생성
GET    /api/v1/users/{id}           # 관리자 상세 조회
PUT    /api/v1/users/{id}           # 관리자 정보 수정
PATCH  /api/v1/users/{id}/role      # 권한 변경
DELETE /api/v1/users/{id}           # 관리자 삭제 (soft delete)
```

---

## 6. API 설계 가이드

### 6.1 RESTful 규칙
- `GET`: 조회 / `POST`: 생성 / `PUT`: 전체 수정 / `PATCH`: 부분 수정 / `DELETE`: 삭제

### 6.2 URL 네이밍
- 복수형 명사 사용: `/api/users`, `/api/reports`
- 케밥 케이스: `/api/user-profiles`
- 버전 관리: `/api/v1/...`

### 6.3 공통 응답 형식

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

### 6.4 페이지네이션 응답

```kotlin
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
```

---

## 7. 에러 처리 전략

### 7.1 예외 계층

```kotlin
sealed class BusinessException(
    val errorCode: String,
    message: String
) : RuntimeException(message)

class ResourceNotFoundException(message: String)
    : BusinessException("NOT_FOUND", message)

class ValidationException(message: String)
    : BusinessException("VALIDATION_ERROR", message)

class UnauthorizedException(message: String)
    : BusinessException("UNAUTHORIZED", message)

class ForbiddenException(message: String)
    : BusinessException("FORBIDDEN", message)
```

### 7.2 에러 응답 형식

```kotlin
data class ErrorResponse(
    val success: Boolean = false,
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

### 7.3 GlobalExceptionHandler
- `@RestControllerAdvice`로 전역 예외 처리
- 적절한 HTTP 상태 코드 반환
- 일관된 에러 응답 형식 유지

---

## 8. 데이터베이스 전략

### 8.1 PostgreSQL 사용

### 8.2 JPA Entity 작성 규칙

```kotlin
@Entity
@Table(name = "admin_users")
class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: AdminRole = AdminRole.VIEWER,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 8.3 Repository 패턴
- Spring Data JPA 인터페이스 활용
- 복잡한 쿼리는 `@Query` 또는 QueryDSL 사용

### 8.4 성능 최적화
- N+1 문제 방지: Fetch Join, EntityGraph 활용
- 불필요한 쿼리 최소화: DTO Projection 활용
- 트랜잭션 범위 최소화: `readOnly` 트랜잭션 활용
- 인덱스 전략: 자주 조회되는 컬럼에 인덱스 설정

---

## 9. 보안 가이드

### 9.1 인증/인가
- Spring Security + JWT 기반 인증
- Role 기반 접근 제어 (RBAC)
- SecurityConfig에서 엔드포인트별 권한 설정

### 9.2 민감 정보 처리
- 비밀번호는 BCrypt 해싱
- API 키, DB 비밀번호는 환경변수 또는 외부 설정 사용
- `application.yaml`에 민감 정보 직접 기록 금지

### 9.3 API 보안
- CORS 설정: 허용된 프론트엔드 도메인만 접근
- CSRF: REST API이므로 비활성화 (JWT 사용)
- Rate Limiting 적용 고려

---

## 10. 테스트 전략

### 10.1 TDD 프로세스
1. 실패하는 테스트 작성 (Red)
2. 테스트 통과하는 최소 코드 작성 (Green)
3. 리팩토링 (Refactor)

### 10.2 테스트 레이어
- **Unit Test**: Service 계층 로직 (MockK 사용)
- **Integration Test**: Repository, API 통합 테스트 (`@SpringBootTest`)
- **API Test**: Controller 엔드포인트 (`@WebMvcTest`)

### 10.3 테스트 네이밍 & 패턴
- 한글 또는 영문 명확한 설명
- Given-When-Then 패턴

```kotlin
@Test
fun `유효한 자격증명으로 로그인하면 JWT 토큰을 반환한다`() {
    // given
    val request = LoginRequest(email = "admin@example.com", password = "password")

    // when
    val result = authService.login(request)

    // then
    assertThat(result.accessToken).isNotBlank()
    assertThat(result.refreshToken).isNotBlank()
}
```

---

## 11. 로깅 전략

### 11.1 로그 레벨
| 레벨 | 용도 |
|------|------|
| ERROR | 시스템 오류, 예외 상황 |
| WARN | 주의가 필요한 상황 |
| INFO | 주요 비즈니스 로직 실행 |
| DEBUG | 개발 시 디버깅 정보 |

### 11.2 로깅 가이드

```kotlin
private val logger = KotlinLogging.logger {}

logger.info { "Admin login success: email=${admin.email}" }
logger.error(e) { "Login failed: ${e.message}" }
```

---

## 12. 코드 리뷰 체크리스트

- [ ] 비즈니스 로직이 Service(Domain) 계층에 있는가?
- [ ] Controller는 DTO 변환과 위임만 담당하는가?
- [ ] null-safety가 보장되는가?
- [ ] 예외 처리가 적절한가?
- [ ] 테스트가 작성되었는가? (TDD)
- [ ] 기능 명세(Spec)가 먼저 작성·합의되었는가? (SDD)
- [ ] API 문서(Swagger/OpenAPI)가 업데이트되었는가?
- [ ] 트랜잭션 경계가 명확한가?
- [ ] DDD 경계(Bounded Context)가 지켜졌는가?

---

## 13. 개발 로드맵

### Phase 1: 기반 구축 및 인증
- [ ] 프로젝트 초기 설정 (Spring Boot + Gradle Kotlin DSL)
- [ ] DDD 패키지 구조 세팅
- [ ] PostgreSQL 연동 및 JPA 설정
- [ ] Spring Security + JWT 인증 구현
- [ ] 로그인/로그아웃 API
- [ ] RBAC 권한 관리
- [ ] CORS 설정 (React 프론트 연동)
- [ ] 전역 예외 처리 (GlobalExceptionHandler)
- [ ] 공통 응답 형식 정의

### Phase 2: 핵심 기능
- [ ] 관리자 사용자 CRUD
- [ ] 보고서 도메인 설계
- [ ] 보고서 조회/생성 API
- [ ] 보고서 필터링 및 페이지네이션
- [ ] 보고서 내보내기 (CSV, Excel)

### Phase 3: 고도화
- [ ] 보고서 템플릿 기능
- [ ] 대시보드 통계 API
- [ ] 감사 로그 (Audit Log)
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 성능 최적화 및 모니터링

---

## 14. 비기능 요구사항

| 항목 | 목표 |
|------|------|
| 응답 시간 | 일반 API 200ms 이내, 보고서 조회 1s 이내 |
| 가용성 | 99.9% 이상 |
| 보안 | OWASP Top 10 대응 |
| 테스트 커버리지 | 핵심 비즈니스 로직 80% 이상 |
| API 문서화 | 모든 엔드포인트 Swagger 문서화 |
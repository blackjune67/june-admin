# 동적 RBAC 시스템 설계 문서

## 목차
1. [개요](#개요)
2. [엔티티 설계](#엔티티-설계)
3. [테이블 설계](#테이블-설계)
4. [관계 다이어그램](#관계-다이어그램)
5. [Spring Security 통합](#spring-security-통합)
6. [마이그레이션 전략](#마이그레이션-전략)

---

## 개요

### 현재 상태
기존 시스템은 `AdminRole` enum을 통한 정적 역할 관리:
```kotlin
enum class AdminRole {
    SUPER_ADMIN,
    ADMIN,
    MANAGER,
    VIEWER
}
```

**문제점:**
- 새로운 역할 추가 시 코드 변경 및 재배포 필요
- 권한 수정이 불가능 (enum 기반)
- 메뉴 접근 제어가 하드코딩되어 있음
- 유연한 권한 관리 불가능

### 전환 목표
동적 RBAC(Role-Based Access Control) 시스템 구현:
- **역할(Role)**: 데이터베이스에서 동적으로 생성/수정/삭제
- **권한(Permission)**: 리소스+액션 기반의 세분화된 권한 관리
- **메뉴(Menu)**: 사용자 역할에 따른 동적 메뉴 트리 생성
- **실시간 적용**: 코드 배포 없이 권한 변경 즉시 반영
- **하위호환성**: 기존 enum과 DB 역할 병행 가능

### 핵심 개념

#### 역할 (Role)
- 권한의 집합을 논리적으로 그룹화
- 시스템 역할: 변경 불가능한 기본 역할 (SUPER_ADMIN, ADMIN 등)
- 커스텀 역할: 관리자가 생성한 사용자 정의 역할

#### 권한 (Permission)
- 리소스(Resource): 관리 대상 (사용자, 역할, 메뉴 등)
- 액션(Action): 수행 가능한 작업 (CREATE, READ, UPDATE, DELETE, EXECUTE 등)
- 권한 = Resource + Action 조합

#### 메뉴 (Menu)
- 계층적 구조 (self-referencing tree)
- 권한에 따른 동적 메뉴 생성
- 사용자가 접근 가능한 메뉴만 노출

---

## 엔티티 설계

### 1. Permission (권한)
```kotlin
@Entity
@Table(name = "permissions", uniqueConstraints = [
    UniqueConstraint(columnNames = ["resource", "action"])
])
data class Permission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val resource: String,  // e.g., "users", "roles", "menus"

    @Column(nullable = false, length = 100)
    val action: String,  // e.g., "CREATE", "READ", "UPDATE", "DELETE", "EXECUTE"

    @Column(nullable = false, length = 255)
    val description: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(mappedBy = "permissions", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val roles: MutableSet<Role> = mutableSetOf()
)
```

**용도:**
- 권한의 원자 단위
- 전체 권한 목록 관리
- 역할에 할당되는 권한의 기준

---

### 2. Role (역할)
```kotlin
@Entity
@Table(name = "roles", uniqueConstraints = [
    UniqueConstraint(columnNames = ["code"])
])
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100, unique = true)
    val code: String,  // e.g., "ROLE_ADMIN", "ROLE_MANAGER"

    @Column(nullable = false, length = 255)
    val name: String,  // e.g., "관리자", "매니저"

    @Column(length = 500)
    val description: String? = null,

    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false,  // true: 기본 역할, 수정 불가; false: 커스텀 역할

    @Column(name = "is_enabled", nullable = false)
    val isEnabled: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    val permissions: MutableSet<Permission> = mutableSetOf(),

    @OneToMany(mappedBy = "role", cascade = [CascadeType.ALL], orphanRemoval = true)
    val menus: MutableSet<RoleMenu> = mutableSetOf()
)
```

**용도:**
- 권한의 논리적 집합
- 사용자에게 할당되는 단위
- 시스템 역할은 변경 불가능하게 보호

---

### 3. Menu (메뉴)
```kotlin
@Entity
@Table(name = "menus")
data class Menu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val name: String,  // e.g., "사용자 관리", "역할 관리"

    @Column(nullable = false, length = 500)
    val path: String,  // e.g., "/admin/users", "/admin/roles"

    @Column(length = 100)
    val icon: String? = null,  // e.g., "users", "shield"

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: Menu? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val children: MutableSet<Menu> = mutableSetOf(),

    @Column(name = "required_permission", length = 255)
    val requiredPermission: String? = null,  // e.g., "users:READ" (permission code)

    @Column(name = "is_hidden", nullable = false)
    val isHidden: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "menu", cascade = [CascadeType.ALL], orphanRemoval = true)
    val roleMenus: MutableSet<RoleMenu> = mutableSetOf()
)
```

**용도:**
- 계층적 메뉴 구조
- 사용자 인터페이스의 네비게이션
- 권한 기반 메뉴 필터링

---

### 4. RolePermission (역할-권한 관계) - Junction Table
```kotlin
@Entity
@Table(name = "role_permissions", uniqueConstraints = [
    UniqueConstraint(columnNames = ["role_id", "permission_id"])
])
data class RolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    val role: Role,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    val permission: Permission,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

**Note:** `@ManyToMany`로 선언하면 Kotlin DSL에서 자동으로 생성됨.

---

### 5. RoleMenu (역할-메뉴 관계) - Junction Table
```kotlin
@Entity
@Table(name = "role_menus", uniqueConstraints = [
    UniqueConstraint(columnNames = ["role_id", "menu_id"])
])
data class RoleMenu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    val role: Role,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu,

    @Column(name = "is_visible", nullable = false)
    val isVisible: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

**용도:**
- 특정 역할이 어떤 메뉴에 접근 가능한지 관리
- 메뉴 가시성 제어

---

### 6. User 엔티티 확장
```kotlin
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 100)
    val username: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<Role> = mutableSetOf(),

    // ... 기존 필드들
)
```

---

## 테이블 설계

### 1. permissions
```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_action (resource, action),
    INDEX idx_resource (resource),
    INDEX idx_action (action)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 권한 고유 ID |
| `resource` | VARCHAR(100) | NOT NULL, UNIQUE (with action) | 리소스 (예: users, roles) |
| `action` | VARCHAR(100) | NOT NULL, UNIQUE (with resource) | 액션 (예: CREATE, READ) |
| `description` | VARCHAR(255) | NOT NULL | 권한 설명 |
| `created_at` | TIMESTAMP | NOT NULL | 생성 시간 |

---

### 2. roles
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_is_system (is_system)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | BIGINT | PK | 역할 고유 ID |
| `code` | VARCHAR(100) | NOT NULL, UNIQUE | 역할 코드 (예: ROLE_ADMIN) |
| `name` | VARCHAR(255) | NOT NULL | 역할명 (예: 관리자) |
| `description` | VARCHAR(500) | NULL | 역할 설명 |
| `is_system` | BOOLEAN | NOT NULL | 시스템 역할 여부 |
| `is_enabled` | BOOLEAN | NOT NULL | 활성화 여부 |
| `created_at` | TIMESTAMP | NOT NULL | 생성 시간 |
| `updated_at` | TIMESTAMP | NULL | 수정 시간 |

---

### 3. role_permissions
```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    INDEX idx_permission_id (permission_id)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `role_id` | BIGINT | PK, FK | 역할 ID |
| `permission_id` | BIGINT | PK, FK | 권한 ID |

---

### 4. menus
```sql
CREATE TABLE menus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    icon VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    parent_id BIGINT,
    required_permission VARCHAR(255),
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES menus(id) ON DELETE CASCADE,
    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | BIGINT | PK | 메뉴 고유 ID |
| `name` | VARCHAR(255) | NOT NULL | 메뉴명 |
| `path` | VARCHAR(500) | NOT NULL | 메뉴 경로 |
| `icon` | VARCHAR(100) | NULL | 아이콘 이름 |
| `display_order` | INT | NOT NULL | 표시 순서 |
| `parent_id` | BIGINT | FK, NULL | 부모 메뉴 ID |
| `required_permission` | VARCHAR(255) | NULL | 필요 권한 (예: users:READ) |
| `is_hidden` | BOOLEAN | NOT NULL | 숨김 여부 |
| `created_at` | TIMESTAMP | NOT NULL | 생성 시간 |
| `updated_at` | TIMESTAMP | NULL | 수정 시간 |

---

### 5. role_menus
```sql
CREATE TABLE role_menus (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    INDEX idx_menu_id (menu_id)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `role_id` | BIGINT | PK, FK | 역할 ID |
| `menu_id` | BIGINT | PK, FK | 메뉴 ID |
| `is_visible` | BOOLEAN | NOT NULL | 메뉴 가시성 |
| `created_at` | TIMESTAMP | NOT NULL | 생성 시간 |

---

### 6. user_roles
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    INDEX idx_role_id (role_id)
);
```

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `user_id` | BIGINT | PK, FK | 사용자 ID |
| `role_id` | BIGINT | PK, FK | 역할 ID |

---

## 관계 다이어그램

### Entity Relationship Diagram (Text)

```
┌─────────────┐
│   User      │
├─────────────┤
│ id (PK)     │
│ username    │
│ password    │
│ email       │
└─────────────┘
       │
       │ N:M
       │ user_roles
       │
       ▼
┌─────────────┐
│   Role      │
├─────────────┤
│ id (PK)     │
│ code        │ (unique)
│ name        │
│ description │
│ is_system   │
│ is_enabled  │
│ created_at  │
│ updated_at  │
└─────────────┘
       │
       ├─── N:M (role_permissions) ───┐
       │                              │
       │                         ┌─────────────┐
       │                         │ Permission  │
       │                         ├─────────────┤
       │                         │ id (PK)     │
       │                         │ resource    │ (part of UK)
       │                         │ action      │ (part of UK)
       │                         │ description │
       │                         │ created_at  │
       │                         └─────────────┘
       │
       └─── N:M (role_menus) ───┐
                                │
                           ┌─────────────┐
                           │   Menu      │
                           ├─────────────┤
                           │ id (PK)     │
                           │ name        │
                           │ path        │
                           │ icon        │
                           │ display_... │
                           │ parent_id   │ (FK, self-ref)
                           │ required... │
                           │ is_hidden   │
                           │ created_at  │
                           │ updated_at  │
                           └─────────────┘
                                │
                                │ parent_id
                                │ (tree structure)
                                ▼
                           ┌─────────────┐
                           │   Menu      │
                           │  (parent)   │
                           └─────────────┘
```

### 관계 설명

1. **User ← 1:N → Role (user_roles)**
   - 하나의 사용자는 여러 역할을 가질 수 있음
   - 하나의 역할은 여러 사용자에게 할당될 수 있음

2. **Role ← 1:N → Permission (role_permissions)**
   - 하나의 역할은 여러 권한을 가질 수 있음
   - 하나의 권한은 여러 역할에 할당될 수 있음

3. **Role ← 1:N → Menu (role_menus)**
   - 하나의 역할은 여러 메뉴에 접근 가능
   - 하나의 메뉴는 여러 역할이 접근 가능

4. **Menu (Self-Referencing)**
   - 메뉴는 계층적 구조를 형성
   - `parent_id`를 통해 부모 메뉴 지정

---

## Spring Security 통합

### 1. JWT와 권한 저장 전략

#### JWT 토큰 구조
```json
{
  "sub": "user123",
  "username": "john.doe",
  "iat": 1708102800,
  "exp": 1708189200,
  "roles": ["ROLE_ADMIN", "ROLE_MANAGER"]
}
```

**전략:**
- JWT에는 역할 **코드**만 저장 (payload 크기 최소화)
- 권한(Permission) 상세는 JWT에 저장하지 않음
- `@PreAuthorize` 평가 시 실시간으로 DB에서 권한 조회

#### 장점
- JWT 크기 최소화 (네트워크 효율성)
- 권한 변경이 즉시 반영됨 (토큰 재발급 불필요)
- 권한 불일치 문제 해결

---

### 2. SecurityConfig 구현

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val userDetailsService: UserDetailsService,
    private val authenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val accessDeniedHandler: JwtAccessDeniedHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                  .accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/auth/**").permitAll()
                  .requestMatchers("/health").permitAll()
                  .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider),
                             UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration):
        AuthenticationManager = authenticationConfiguration.authenticationManager
}
```

---

### 3. UserDetailsService with Dynamic Permissions

```kotlin
@Service
class RbacUserDetailsService(
    private val userRepository: UserRepository,
    private val permissionRepository: PermissionRepository,
    private val roleRepository: RoleRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        // DB에서 역할 조회
        val roles = user.roles.map { it.code }.toSet()

        // 역할에 해당하는 모든 권한 조회 (실시간)
        val permissions = if (roles.contains("ROLE_SUPER_ADMIN")) {
            // SUPER_ADMIN은 모든 권한
            permissionRepository.findAll()
        } else {
            permissionRepository.findByRolesIn(user.roles)
        }

        val authorities = permissions.map { permission ->
            SimpleGrantedAuthority("${permission.resource}:${permission.action}")
        } + roles.map { role ->
            SimpleGrantedAuthority(role)
        }

        return User(
            user.username,
            user.password,
            authorities
        ).apply {
            // 추가 정보 저장 (선택)
            this.isAccountNonExpired = true
            this.isAccountNonLocked = true
            this.isCredentialsNonExpired = true
            this.isEnabled = true
        }
    }
}
```

---

### 4. @PreAuthorize 예제

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    // 방법 1: 역할 기반 접근 제어
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun getAllUsers(): ResponseEntity<ApiResponse<List<UserDto>>> {
        // ...
    }

    // 방법 2: 권한 기반 접근 제어
    @PostMapping
    @PreAuthorize("hasAuthority('users:CREATE')")
    fun createUser(@RequestBody dto: CreateUserRequest): ResponseEntity<ApiResponse<UserDto>> {
        // ...
    }

    // 방법 3: 복합 조건
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:UPDATE') or @rbacService.isOwnProfile(#id)")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody dto: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserDto>> {
        // ...
    }

    // 방법 4: 역할 OR 권한
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasAuthority('users:DELETE')")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<ApiResponse<Void>> {
        // ...
    }
}
```

---

### 5. PermissionEvaluator 커스텀 구현

```kotlin
@Component("rbacService")
class RbacPermissionEvaluator(
    private val permissionRepository: PermissionRepository,
    private val userRepository: UserRepository
) : PermissionEvaluator {

    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any,
        permission: String
    ): Boolean {
        val principal = authentication.principal as? UserDetails ?: return false
        val userId = getUserIdFromPrincipal(principal) ?: return false

        return userRepository.findById(userId)
            .map { user ->
                val (resource, action) = permission.split(":").let {
                    Pair(it.getOrNull(0) ?: "", it.getOrNull(1) ?: "")
                }
                permissionRepository.existsByResourceAndActionAndRolesIn(
                    resource, action, user.roles
                )
            }
            .orElse(false)
    }

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: String
    ): Boolean {
        // targetId 기반 접근 제어 (예: 자신의 프로필만 수정 가능)
        val principal = authentication.principal as? UserDetails ?: return false
        val userId = getUserIdFromPrincipal(principal) ?: return false

        return when (targetType) {
            "User" -> userId == (targetId as Long)
            else -> false
        }
    }

    fun isOwnProfile(userId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as? UserDetails ?: return false
        val currentUserId = getUserIdFromPrincipal(principal) ?: return false
        return currentUserId == userId
    }

    private fun getUserIdFromPrincipal(principal: UserDetails): Long? {
        // principal에서 userId 추출 (구현 방식은 프로젝트에 따라 다름)
        return null
    }
}
```

---

### 6. JWT 토큰 생성 시 역할 포함

```kotlin
@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}")
    private val jwtSecret: String,

    @Value("\${app.jwt.expiration}")
    private val jwtExpirationMs: Int
) {

    fun generateToken(authentication: Authentication): String {
        val userDetails = authentication.principal as UserDetails
        val user = getUserEntityFromDetails(userDetails)

        val roles = user.roles.map { it.code }

        val issuedAt = Date()
        val expiryDate = Date(issuedAt.time + jwtExpirationMs)

        return Jwts.builder()
            .setSubject(userDetails.username)
            .claim("roles", roles)
            .setIssuedAt(issuedAt)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact()
    }

    fun getRolesFromToken(token: String): List<String> {
        val claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .body

        @Suppress("UNCHECKED_CAST")
        return (claims["roles"] as? List<String>) ?: emptyList()
    }

    private fun getUserEntityFromDetails(userDetails: UserDetails): User {
        // UserDetails에서 User 엔티티 추출
        // 구현은 프로젝트에 따라 다름
        return User()
    }
}
```

---

## 마이그레이션 전략

### Phase 1: 기본 인프라 구축

#### Step 1.1: 엔티티 및 테이블 생성
- Permission, Role, Menu, RolePermission, RoleMenu 엔티티 생성
- Flyway 또는 Liquibase를 통한 DB 마이그레이션

#### Step 1.2: 저장소 및 서비스 구현
- Repository 인터페이스 구현
- RoleService, PermissionService, MenuService 구현

#### Step 1.3: RBAC 데이터 초기화
```kotlin
@Component
class RbacDataInitializer(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val menuRepository: MenuRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        initializePermissions()
        initializeRoles()
        initializeMenus()
    }

    private fun initializePermissions() {
        val permissions = listOf(
            Permission(resource = "users", action = "CREATE", description = "사용자 생성"),
            Permission(resource = "users", action = "READ", description = "사용자 조회"),
            Permission(resource = "users", action = "UPDATE", description = "사용자 수정"),
            Permission(resource = "users", action = "DELETE", description = "사용자 삭제"),
            Permission(resource = "roles", action = "CREATE", description = "역할 생성"),
            Permission(resource = "roles", action = "READ", description = "역할 조회"),
            Permission(resource = "roles", action = "UPDATE", description = "역할 수정"),
            Permission(resource = "roles", action = "DELETE", description = "역할 삭제"),
            Permission(resource = "menus", action = "READ", description = "메뉴 조회"),
            // ...
        )
        permissionRepository.saveAll(permissions)
    }

    private fun initializeRoles() {
        val superAdminPermissions = permissionRepository.findAll()
        val superAdminRole = Role(
            code = "ROLE_SUPER_ADMIN",
            name = "슈퍼 관리자",
            isSystem = true,
            permissions = superAdminPermissions.toMutableSet()
        )
        roleRepository.save(superAdminRole)

        val adminPermissions = permissionRepository.findAll()
            .filter { it.resource != "admin" }
            .toMutableSet()
        val adminRole = Role(
            code = "ROLE_ADMIN",
            name = "관리자",
            isSystem = true,
            permissions = adminPermissions
        )
        roleRepository.save(adminRole)

        val managerPermissions = permissionRepository.findAll()
            .filter { it.resource in listOf("users", "menus") && it.action in listOf("READ", "UPDATE") }
            .toMutableSet()
        val managerRole = Role(
            code = "ROLE_MANAGER",
            name = "매니저",
            isSystem = true,
            permissions = managerPermissions
        )
        roleRepository.save(managerRole)

        val viewerPermissions = permissionRepository.findAll()
            .filter { it.action == "READ" }
            .toMutableSet()
        val viewerRole = Role(
            code = "ROLE_VIEWER",
            name = "조회자",
            isSystem = true,
            permissions = viewerPermissions
        )
        roleRepository.save(viewerRole)
    }

    private fun initializeMenus() {
        // 메인 메뉴
        val adminMenu = Menu(
            name = "관리",
            path = "/admin",
            icon = "shield",
            displayOrder = 1,
            isHidden = false
        )

        // 자식 메뉴
        val usersMenu = Menu(
            name = "사용자 관리",
            path = "/admin/users",
            icon = "users",
            displayOrder = 1,
            parent = adminMenu,
            requiredPermission = "users:READ"
        )

        val rolesMenu = Menu(
            name = "역할 관리",
            path = "/admin/roles",
            icon = "shield-alt",
            displayOrder = 2,
            parent = adminMenu,
            requiredPermission = "roles:READ"
        )

        menuRepository.saveAll(listOf(adminMenu, usersMenu, rolesMenu))
    }
}
```

---

### Phase 2: 이중 모드 구현 (하위호환성)

#### Step 2.1: Enum 호환성 유지
```kotlin
object LegacyAdminRoleConverter {
    fun adminRoleEnumToRoleEntity(enumRole: AdminRole): Role {
        return when (enumRole) {
            AdminRole.SUPER_ADMIN -> roleRepository.findByCode("ROLE_SUPER_ADMIN")
            AdminRole.ADMIN -> roleRepository.findByCode("ROLE_ADMIN")
            AdminRole.MANAGER -> roleRepository.findByCode("ROLE_MANAGER")
            AdminRole.VIEWER -> roleRepository.findByCode("ROLE_VIEWER")
        } ?: throw IllegalArgumentException("Role not found for enum: $enumRole")
    }
}
```

#### Step 2.2: 이중 모드 UserDetailsService
```kotlin
@Service
class HybridRbacUserDetailsService(
    private val rbacUserDetailsService: RbacUserDetailsService,
    private val legacyUserDetailsService: LegacyUserDetailsService,
    @Value("\${app.rbac.enabled:true}")
    private val rbacEnabled: Boolean
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        return if (rbacEnabled) {
            rbacUserDetailsService.loadUserByUsername(username)
        } else {
            legacyUserDetailsService.loadUserByUsername(username)
        }
    }
}
```

#### Step 2.3: 점진적 마이그레이션
```yaml
# application.yaml
app:
  rbac:
    enabled: true  # true = 새 시스템, false = 기존 enum 시스템
    migration:
      phase: 2  # 1: 준비, 2: 이중모드, 3: 완전전환
```

---

### Phase 3: 기존 데이터 마이그레이션

#### Step 3.1: AdminRole enum 사용자 → RBAC 역할 매핑
```kotlin
@Component
class AdminRoleToRbacMigrator(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    @Transactional
    fun migrateAdminRoles() {
        // Step 1: 모든 사용자 조회
        val users = userRepository.findAll()

        users.forEach { user ->
            // Step 2: enum 역할을 DB Role로 변환
            val dbRoles = user.adminRoles.map { enumRole ->
                val roleCode = "ROLE_${enumRole.name}"
                roleRepository.findByCode(roleCode)
                    ?: throw IllegalArgumentException("Role not found: $roleCode")
            }.toMutableSet()

            // Step 3: 사용자에게 DB Role 할당
            user.roles.addAll(dbRoles)
            userRepository.save(user)
        }

        log.info("Migrated ${users.size} users from AdminRole enum to RBAC roles")
    }
}
```

#### Step 3.2: 마이그레이션 실행 API
```kotlin
@RestController
@RequestMapping("/api/v1/admin/migration")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
class MigrationController(
    private val adminRoleToRbacMigrator: AdminRoleToRbacMigrator
) {

    @PostMapping("/migrate-roles")
    fun migrateRoles(): ResponseEntity<ApiResponse<MigrationResult>> {
        val result = adminRoleToRbacMigrator.migrateAdminRoles()
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = result,
                message = "역할 마이그레이션 완료"
            )
        )
    }
}
```

---

### Phase 4: 기존 코드 점진적 제거

#### Step 4.1: 배포별 전환
- **배포 1-3**: RBAC 시스템 운영, enum 시스템 병행
- **배포 4**: enum 기반 접근 제어 코드 제거
- **배포 5**: AdminRole enum 자체 제거

#### Step 4.2: 기능 플래그를 통한 점진적 전환
```kotlin
@Component
class RbacFeatureToggle(
    private val featureFlagService: FeatureFlagService
) {

    fun useRbac(): Boolean = featureFlagService.isEnabled("rbac.enabled")

    fun canDeleteEnumRole(): Boolean = featureFlagService.isEnabled("enum.role.delete")
}
```

---

### 마이그레이션 체크리스트

- [ ] Permission, Role, Menu 엔티티 및 테이블 생성
- [ ] Repository, Service, Controller 구현
- [ ] RbacDataInitializer를 통한 초기 데이터 로드
- [ ] SecurityConfig 및 JwtTokenProvider 수정
- [ ] RbacUserDetailsService 구현
- [ ] 기존 코드와 호환성 검증
- [ ] @PreAuthorize 예제 테스트
- [ ] AdminRole enum 사용자 데이터 마이그레이션
- [ ] 기존 enum 기반 접근 제어 코드 제거
- [ ] 전체 통합 테스트 및 성능 테스트
- [ ] AdminRole enum 제거
- [ ] 모니터링 및 로깅 검증

---

## 참고사항

### 성능 최적화
1. **권한 조회 캐싱**: `@Cacheable` 어노테이션 사용
2. **Role 페칭 전략**: `FetchType.LAZY` 사용, N+1 문제 주의
3. **데이터베이스 인덱싱**: resource, action, code, role_id, permission_id에 인덱스 생성
4. **권한 조회 쿼리 최적화**: `@Query`로 직접 작성

### 보안 고려사항
1. **시스템 역할 보호**: `is_system = true` 역할은 수정 불가
2. **감시 로깅**: 모든 권한 변경 사항 기록
3. **권한 상승 방지**: 사용자는 자신의 권한보다 높은 권한을 할당 불가
4. **감시 감사(Audit)**: 누가, 언제, 어떤 권한을 변경했는지 기록

### 확장성
1. **권한 캐싱 무효화 전략**: Redis 활용
2. **멀티테넌트 지원**: `tenant_id` 컬럼 추가 고려
3. **임시 권한**: 시간 제한이 있는 권한 지원
4. **권한 위임**: 사용자가 다른 사용자에게 일부 권한 위임 가능


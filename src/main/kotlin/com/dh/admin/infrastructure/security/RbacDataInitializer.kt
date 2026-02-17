package com.dh.admin.infrastructure.security

import com.dh.admin.domain.role.entity.Permission
import com.dh.admin.domain.role.entity.Role
import com.dh.admin.domain.role.repository.PermissionRepository
import com.dh.admin.domain.role.repository.RoleRepository
import com.dh.admin.domain.user.entity.AdminRole
import com.dh.admin.domain.user.repository.AdminUserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RbacDataInitializer(
    private val permissionRepository: PermissionRepository,
    private val roleRepository: RoleRepository,
    private val adminUserRepository: AdminUserRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (roleRepository.count() > 0) {
            log.info("RBAC 데이터가 이미 존재합니다. 초기화를 건너뜁니다.")
            migrateExistingUsers()
            return
        }

        log.info("RBAC 초기 데이터를 생성합니다...")

        val permissions = seedPermissions()
        val roles = seedRoles(permissions)
        migrateExistingUsers()

        log.info("RBAC 초기화 완료: 권한 ${permissions.size}개, 역할 ${roles.size}개")
    }

    private fun seedPermissions(): List<Permission> {
        val permissionDefs = listOf(
            // 사용자 관리
            Triple("user", "create", "사용자 생성"),
            Triple("user", "read", "사용자 조회"),
            Triple("user", "update", "사용자 수정"),
            Triple("user", "delete", "사용자 삭제"),
            // 역할 관리
            Triple("role", "create", "역할 생성"),
            Triple("role", "read", "역할 조회"),
            Triple("role", "update", "역할 수정"),
            Triple("role", "delete", "역할 삭제"),
            // 권한 관리
            Triple("permission", "create", "권한 생성"),
            Triple("permission", "read", "권한 조회"),
            Triple("permission", "update", "권한 수정"),
            Triple("permission", "delete", "권한 삭제"),
            // 메뉴 관리
            Triple("menu", "create", "메뉴 생성"),
            Triple("menu", "read", "메뉴 조회"),
            Triple("menu", "update", "메뉴 수정"),
            Triple("menu", "delete", "메뉴 삭제")
        )

        return permissionDefs.map { (resource, action, name) ->
            permissionRepository.findByResourceAndAction(resource, action)
                ?: permissionRepository.save(Permission(resource = resource, action = action, name = name))
        }
    }

    private fun seedRoles(permissions: List<Permission>): List<Role> {
        val allPermissions = permissions.toSet()

        val readPermissions = permissions.filter { it.action == "read" }.toSet()

        val adminPermissions = permissions.filter {
            it.resource in listOf("user", "menu") || it.action == "read"
        }.toSet()

        val superAdmin = roleRepository.findByCode("SUPER_ADMIN") ?: roleRepository.save(
            Role(code = "SUPER_ADMIN", name = "최고 관리자", description = "모든 권한을 가진 최고 관리자", isSystem = true)
        ).also { it.permissions.addAll(allPermissions) }

        val admin = roleRepository.findByCode("ADMIN") ?: roleRepository.save(
            Role(code = "ADMIN", name = "관리자", description = "일반 관리자", isSystem = true)
        ).also { it.permissions.addAll(adminPermissions) }

        val viewer = roleRepository.findByCode("VIEWER") ?: roleRepository.save(
            Role(code = "VIEWER", name = "뷰어", description = "읽기 전용 사용자", isSystem = true)
        ).also { it.permissions.addAll(readPermissions) }

        return listOf(superAdmin, admin, viewer)
    }

    private fun migrateExistingUsers() {
        val users = adminUserRepository.findAll()
        var migratedCount = 0

        users.forEach { user ->
            if (user.roles.isEmpty() && user.role != null) {
                val roleCode = user.role!!.name
                val role = roleRepository.findByCode(roleCode)
                if (role != null) {
                    user.roles.add(role)
                    adminUserRepository.save(user)
                    migratedCount++
                    log.info("사용자 마이그레이션: ${user.email} → 역할: $roleCode")
                }
            }
        }

        if (migratedCount > 0) {
            log.info("기존 사용자 $migratedCount 명의 역할을 마이그레이션했습니다.")
        }
    }
}

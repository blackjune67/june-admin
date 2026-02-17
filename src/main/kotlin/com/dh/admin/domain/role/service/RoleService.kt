package com.dh.admin.domain.role.service

import com.dh.admin.application.dto.*
import com.dh.admin.common.exception.DuplicateException
import com.dh.admin.common.exception.ForbiddenException
import com.dh.admin.common.exception.ResourceNotFoundException
import com.dh.admin.domain.role.entity.Role
import com.dh.admin.domain.role.repository.PermissionRepository
import com.dh.admin.domain.role.repository.RoleRepository
import com.dh.admin.domain.user.repository.AdminUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RoleService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val adminUserRepository: AdminUserRepository
) {

    @Transactional
    fun create(request: CreateRoleRequest): RoleResponse {
        if (roleRepository.existsByCode(request.code)) {
            throw DuplicateException("이미 존재하는 역할 코드입니다: ${request.code}")
        }

        val role = roleRepository.save(
            Role(
                code = request.code,
                name = request.name,
                description = request.description
            )
        )
        return role.toResponse()
    }

    fun findAll(): List<RoleResponse> {
        return roleRepository.findAll().map { it.toResponse() }
    }

    fun findById(id: Long): RoleResponse {
        val role = roleRepository.findByIdWithPermissions(id)
            ?: throw ResourceNotFoundException("역할을 찾을 수 없습니다: $id")
        return role.toResponseWithPermissions()
    }

    @Transactional
    fun update(id: Long, request: UpdateRoleRequest): RoleResponse {
        val role = roleRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("역할을 찾을 수 없습니다: $id") }

        role.name = request.name
        request.description?.let { role.description = it }
        request.isActive?.let { role.isActive = it }

        return roleRepository.save(role).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val role = roleRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("역할을 찾을 수 없습니다: $id") }

        if (role.isSystem) {
            throw ForbiddenException("시스템 역할은 삭제할 수 없습니다.")
        }

        roleRepository.delete(role)
    }

    @Transactional
    fun assignPermissions(roleId: Long, request: AssignPermissionsRequest): RoleResponse {
        val role = roleRepository.findByIdWithPermissions(roleId)
            ?: throw ResourceNotFoundException("역할을 찾을 수 없습니다: $roleId")

        val permissions = permissionRepository.findAllByIdIn(request.permissionIds)
        role.permissions.clear()
        role.permissions.addAll(permissions)

        return roleRepository.save(role).toResponseWithPermissions()
    }

    fun getUserRoles(userId: Long): List<RoleSummary> {
        val user = adminUserRepository.findByIdWithRolesAndPermissions(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        return user.roles.map { it.toSummary() }
    }

    @Transactional
    fun assignUserRoles(userId: Long, request: AssignRolesRequest) {
        val user = adminUserRepository.findByIdWithRolesAndPermissions(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        val roles = roleRepository.findAllById(request.roleIds)
        user.roles.clear()
        user.roles.addAll(roles)
        adminUserRepository.save(user)
    }

    private fun Role.toResponse() = RoleResponse(
        id = this.id,
        code = this.code,
        name = this.name,
        description = this.description,
        isActive = this.isActive,
        isSystem = this.isSystem
    )

    private fun Role.toResponseWithPermissions() = RoleResponse(
        id = this.id,
        code = this.code,
        name = this.name,
        description = this.description,
        isActive = this.isActive,
        isSystem = this.isSystem,
        permissions = this.permissions.map {
            PermissionResponse(
                id = it.id,
                resource = it.resource,
                action = it.action,
                name = it.name,
                description = it.description,
                authority = it.authority
            )
        }
    )

    private fun Role.toSummary() = RoleSummary(
        id = this.id,
        code = this.code,
        name = this.name
    )
}

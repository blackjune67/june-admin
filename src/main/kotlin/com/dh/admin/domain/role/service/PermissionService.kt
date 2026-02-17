package com.dh.admin.domain.role.service

import com.dh.admin.application.dto.*
import com.dh.admin.common.exception.DuplicateException
import com.dh.admin.common.exception.ResourceNotFoundException
import com.dh.admin.common.exception.ValidationException
import com.dh.admin.domain.role.entity.Permission
import com.dh.admin.domain.role.repository.PermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PermissionService(
    private val permissionRepository: PermissionRepository
) {

    @Transactional
    fun create(request: CreatePermissionRequest): PermissionResponse {
        if (permissionRepository.existsByResourceAndAction(request.resource, request.action)) {
            throw DuplicateException("이미 존재하는 권한입니다: ${request.resource}:${request.action}")
        }

        val permission = permissionRepository.save(
            Permission(
                resource = request.resource,
                action = request.action,
                name = request.name,
                description = request.description
            )
        )
        return permission.toResponse()
    }

    fun findAll(): List<PermissionGroupResponse> {
        val permissions = permissionRepository.findAll()
        return permissions
            .groupBy { it.resource }
            .map { (resource, perms) ->
                PermissionGroupResponse(
                    resource = resource,
                    permissions = perms.map { it.toResponse() }
                )
            }
            .sortedBy { it.resource }
    }

    fun findAllResources(): List<String> {
        return permissionRepository.findAll()
            .map { it.resource }
            .distinct()
            .sorted()
    }

    @Transactional
    fun update(id: Long, request: UpdatePermissionRequest): PermissionResponse {
        val permission = permissionRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("권한을 찾을 수 없습니다: $id") }

        permission.name = request.name
        permission.description = request.description

        return permissionRepository.save(permission).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val permission = permissionRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("권한을 찾을 수 없습니다: $id") }

        try {
            permissionRepository.delete(permission)
            permissionRepository.flush()
        } catch (e: Exception) {
            throw ValidationException("사용 중인 권한은 삭제할 수 없습니다.")
        }
    }

    private fun Permission.toResponse() = PermissionResponse(
        id = this.id,
        resource = this.resource,
        action = this.action,
        name = this.name,
        description = this.description,
        authority = this.authority
    )
}

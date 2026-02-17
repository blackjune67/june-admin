package com.dh.admin.domain.role.repository

import com.dh.admin.domain.role.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByResourceAndAction(resource: String, action: String): Permission?
    fun findByResource(resource: String): List<Permission>
    fun findAllByIdIn(ids: Collection<Long>): List<Permission>
    fun existsByResourceAndAction(resource: String, action: String): Boolean
}

package com.dh.admin.domain.role.repository

import com.dh.admin.domain.role.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByCode(code: String): Role?
    fun existsByCode(code: String): Boolean

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    fun findByIdWithPermissions(id: Long): Role?

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    fun findByCodeWithPermissions(code: String): Role?

    fun findAllByIsActiveTrue(): List<Role>
}

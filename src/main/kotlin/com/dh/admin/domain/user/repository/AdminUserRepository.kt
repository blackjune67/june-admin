package com.dh.admin.domain.user.repository

import com.dh.admin.domain.user.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AdminUserRepository : JpaRepository<AdminUser, Long>, AdminUserQueryRepository {

    fun findByEmail(email: String): AdminUser?

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM AdminUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
    fun findByEmailWithRolesAndPermissions(email: String): AdminUser?

    @Query("SELECT u FROM AdminUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
    fun findByIdWithRolesAndPermissions(id: Long): AdminUser?
}

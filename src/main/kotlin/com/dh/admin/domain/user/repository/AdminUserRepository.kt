package com.dh.admin.domain.user.repository

import com.dh.admin.domain.user.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository

interface AdminUserRepository : JpaRepository<AdminUser, Long> {

    fun findByEmail(email: String): AdminUser?

    fun existsByEmail(email: String): Boolean
}

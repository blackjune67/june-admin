package com.dh.admin.domain.auth.repository

import com.dh.admin.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): RefreshToken?

    fun findByAdminUserId(adminUserId: Long): RefreshToken?

    fun deleteByAdminUserId(adminUserId: Long)
}

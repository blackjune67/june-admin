package com.dh.admin.domain.user.entity

import com.dh.admin.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admin_users")
class AdminUser(
    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: AdminRole = AdminRole.VIEWER,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    var loginFailCount: Int = 0,

    var lockedAt: LocalDateTime? = null
) : BaseEntity() {

    companion object {
        private const val MAX_LOGIN_FAIL_COUNT = 5
    }

    val isLocked: Boolean
        get() = lockedAt != null

    fun increaseLoginFailCount() {
        loginFailCount++
        if (loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
            lockedAt = LocalDateTime.now()
        }
    }

    fun resetLoginFailCount() {
        loginFailCount = 0
        lockedAt = null
    }
}

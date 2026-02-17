package com.dh.admin.domain.user.entity

import com.dh.admin.common.entity.BaseEntity
import com.dh.admin.domain.role.entity.Role
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

    @Deprecated("동적 RBAC으로 전환됨. roles 필드를 사용하세요.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var role: AdminRole? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "admin_user_roles",
        joinColumns = [JoinColumn(name = "admin_user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<Role> = mutableSetOf(),

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

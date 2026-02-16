package com.dh.admin.domain.auth.entity

import com.dh.admin.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(nullable = false)
    val adminUserId: Long,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime
) : BaseEntity() {

    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiresAt)
}

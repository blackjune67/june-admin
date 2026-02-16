package com.dh.admin.infrastructure.security

import com.dh.admin.domain.user.entity.AdminUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val adminUser: AdminUser
) : UserDetails {

    val id: Long get() = adminUser.id
    val email: String get() = adminUser.email
    val name: String get() = adminUser.name
    val role: String get() = adminUser.role.name

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${adminUser.role.name}"))

    override fun getPassword(): String = adminUser.password

    override fun getUsername(): String = adminUser.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = !adminUser.isLocked

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = adminUser.isActive
}

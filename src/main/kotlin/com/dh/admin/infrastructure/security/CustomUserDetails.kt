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

    val roleCodes: List<String>
        get() = adminUser.roles.map { it.code }

    val permissionAuthorities: Set<String>
        get() = adminUser.roles.flatMap { role ->
            role.permissions.map { it.authority }
        }.toSet()

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()

        // 역할 기반 ROLE_ 권한
        adminUser.roles.forEach { role ->
            authorities.add(SimpleGrantedAuthority("ROLE_${role.code}"))
        }

        // resource:action 기반 권한
        adminUser.roles.forEach { role ->
            role.permissions.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission.authority))
            }
        }

        return authorities
    }

    override fun getPassword(): String = adminUser.password

    override fun getUsername(): String = adminUser.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = !adminUser.isLocked

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = adminUser.isActive
}

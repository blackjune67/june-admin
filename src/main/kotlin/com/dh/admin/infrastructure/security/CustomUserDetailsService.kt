package com.dh.admin.infrastructure.security

import com.dh.admin.domain.user.repository.AdminUserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val adminUserRepository: AdminUserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val adminUser = adminUserRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $email")
        return CustomUserDetails(adminUser)
    }
}

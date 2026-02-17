package com.dh.admin.domain.auth.service

import com.dh.admin.application.dto.*
import com.dh.admin.common.exception.DuplicateException
import com.dh.admin.common.exception.UnauthorizedException
import com.dh.admin.common.exception.ValidationException
import com.dh.admin.domain.auth.entity.RefreshToken
import com.dh.admin.domain.auth.exception.AuthExceptionMessages
import com.dh.admin.domain.auth.repository.RefreshTokenRepository
import com.dh.admin.domain.menu.service.MenuService
import com.dh.admin.domain.user.entity.AdminUser
import com.dh.admin.domain.user.repository.AdminUserRepository
import com.dh.admin.infrastructure.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuthService(
    private val adminUserRepository: AdminUserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val menuService: MenuService
) {

    @Transactional
    fun signUp(request: SignUpRequest): AdminUserResponse {
        if (adminUserRepository.existsByEmail(request.email)) {
            throw DuplicateException(AuthExceptionMessages.EMAIL_ALREADY_IN_USE)
        }

        val user = adminUserRepository.save(
            AdminUser(
                email = request.email,
                password = passwordEncoder.encode(request.password)!!,
                name = request.name
            )
        )

        return AdminUserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            roles = user.roles.map { RoleSummary(it.id, it.code, it.name) },
            isActive = user.isActive
        )
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = adminUserRepository.findByEmailWithRolesAndPermissions(request.email)
            ?: throw UnauthorizedException(AuthExceptionMessages.INVALID_CREDENTIALS)

        validateUserState(user)

        if (!passwordEncoder.matches(request.password, user.password)) {
            user.increaseLoginFailCount()
            adminUserRepository.save(user)
            throw UnauthorizedException(AuthExceptionMessages.INVALID_CREDENTIALS)
        }

        user.resetLoginFailCount()
        adminUserRepository.save(user)

        return issueTokens(user)
    }

    @Transactional
    fun refresh(refreshTokenValue: String): TokenResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
            ?: throw UnauthorizedException(AuthExceptionMessages.INVALID_REFRESH_TOKEN)

        if (storedToken.isExpired) {
            refreshTokenRepository.delete(storedToken)
            throw UnauthorizedException(AuthExceptionMessages.EXPIRED_REFRESH_TOKEN)
        }

        val user = adminUserRepository.findByIdWithRolesAndPermissions(storedToken.adminUserId)
            ?: throw UnauthorizedException(AuthExceptionMessages.USER_NOT_FOUND)

        refreshTokenRepository.delete(storedToken)
        return issueTokens(user)
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByAdminUserId(userId)
    }

    fun getMyInfo(userId: Long): MyInfoResponse {
        val user = adminUserRepository.findByIdWithRolesAndPermissions(userId)
            ?: throw UnauthorizedException(AuthExceptionMessages.USER_NOT_FOUND)

        val roles = user.roles.map { RoleSummary(it.id, it.code, it.name) }

        val permissions = user.roles.flatMap { role ->
            role.permissions.map { it.authority }
        }.distinct().sorted()

        val menus = menuService.findAccessibleMenus(permissions.toSet())

        return MyInfoResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            roles = roles,
            permissions = permissions,
            menus = menus
        )
    }

    private fun validateUserState(user: AdminUser) {
        if (!user.isActive) {
            throw ValidationException(AuthExceptionMessages.INACTIVE_ACCOUNT)
        }
        if (user.isLocked) {
            throw ValidationException(AuthExceptionMessages.LOCKED_ACCOUNT)
        }
    }

    private fun issueTokens(user: AdminUser): TokenResponse {
        val roleCodes = user.roles.map { it.code }
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email, roleCodes)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id, user.email, roleCodes)

        refreshTokenRepository.deleteByAdminUserId(user.id)
        refreshTokenRepository.save(
            RefreshToken(
                adminUserId = user.id,
                token = refreshToken,
                expiresAt = LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
            )
        )

        return TokenResponse(accessToken = accessToken, refreshToken = refreshToken)
    }
}

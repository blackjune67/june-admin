package com.dh.admin.domain.auth.service

import com.dh.admin.application.dto.AdminUserResponse
import com.dh.admin.application.dto.LoginRequest
import com.dh.admin.application.dto.SignUpRequest
import com.dh.admin.application.dto.TokenResponse
import com.dh.admin.common.exception.DuplicateException
import com.dh.admin.common.exception.UnauthorizedException
import com.dh.admin.common.exception.ValidationException
import com.dh.admin.domain.auth.entity.RefreshToken
import com.dh.admin.domain.auth.repository.RefreshTokenRepository
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
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun signUp(request: SignUpRequest): AdminUserResponse {
        if (adminUserRepository.existsByEmail(request.email)) {
            throw DuplicateException("이미 사용 중인 이메일입니다.")
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
            role = user.role,
            isActive = user.isActive
        )
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = adminUserRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.")

        validateUserState(user)

        if (!passwordEncoder.matches(request.password, user.password)) {
            user.increaseLoginFailCount()
            adminUserRepository.save(user)
            throw UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        user.resetLoginFailCount()
        adminUserRepository.save(user)

        return issueTokens(user)
    }

    @Transactional
    fun refresh(refreshTokenValue: String): TokenResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
            ?: throw UnauthorizedException("유효하지 않은 Refresh Token입니다.")

        if (storedToken.isExpired) {
            refreshTokenRepository.delete(storedToken)
            throw UnauthorizedException("만료된 Refresh Token입니다. 다시 로그인해주세요.")
        }

        val user = adminUserRepository.findById(storedToken.adminUserId)
            .orElseThrow { UnauthorizedException("사용자를 찾을 수 없습니다.") }

        // 기존 refresh token 삭제 후 새로 발급
        refreshTokenRepository.delete(storedToken)
        return issueTokens(user)
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByAdminUserId(userId)
    }

    fun getMyInfo(userId: Long): AdminUserResponse {
        val user = adminUserRepository.findById(userId)
            .orElseThrow { UnauthorizedException("사용자를 찾을 수 없습니다.") }

        return AdminUserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            isActive = user.isActive
        )
    }

    private fun validateUserState(user: AdminUser) {
        if (!user.isActive) {
            throw ValidationException("비활성화된 계정입니다.")
        }
        if (user.isLocked) {
            throw ValidationException("계정이 잠겨있습니다. 관리자에게 문의하세요.")
        }
    }

    private fun issueTokens(user: AdminUser): TokenResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email, user.role.name)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id, user.email, user.role.name)

        // 기존 refresh token 삭제 후 저장
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

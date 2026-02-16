package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.LoginRequest
import com.dh.admin.application.dto.RefreshRequest
import com.dh.admin.application.dto.SignUpRequest
import com.dh.admin.common.response.ApiResponse
import com.dh.admin.domain.auth.service.AuthService
import com.dh.admin.infrastructure.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<*>> {
        val userResponse = authService.signUp(request)
        return ResponseEntity.status(201).body(ApiResponse.ok(userResponse, "회원가입 성공"))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<*>> {
        val tokenResponse = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse, "로그인 성공"))
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ApiResponse<*>> {
        authService.logout(userDetails.id)
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 성공"))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<ApiResponse<*>> {
        val tokenResponse = authService.refresh(request.refreshToken)
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse, "토큰 갱신 성공"))
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ApiResponse<*>> {
        val userInfo = authService.getMyInfo(userDetails.id)
        return ResponseEntity.ok(ApiResponse.ok(userInfo))
    }
}

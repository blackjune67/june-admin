package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.LoginRequest
import com.dh.admin.application.dto.TokenResponse
import com.dh.admin.common.exception.UnauthorizedException
import com.dh.admin.domain.auth.service.AuthService
import com.dh.admin.infrastructure.security.JwtAuthenticationFilter
import com.dh.admin.interfaces.advice.GlobalExceptionHandler
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class AuthControllerLoginApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `login success returns tokens`() {
        val request = LoginRequest(email = "admin@example.com", password = "password123")
        val response = TokenResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )
        given(authService.login(request)).willReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.message").value("로그인 성공"))

        verify(authService).login(request)
    }

    @Test
    fun `login returns 400 when request is invalid`() {
        val invalidRequest = mapOf(
            "email" to "not-an-email",
            "password" to ""
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errorCode").value("EFC9400"))
            .andExpect(jsonPath("$.detail", containsString("email")))
            .andExpect(jsonPath("$.detail", containsString("password")))
    }

    @Test
    fun `login returns 401 when credentials are invalid`() {
        val request = LoginRequest(email = "admin@example.com", password = "wrong-password")
        given(authService.login(request))
            .willThrow(UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.errorCode").value("EFC9457"))
            .andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다."))
    }
}

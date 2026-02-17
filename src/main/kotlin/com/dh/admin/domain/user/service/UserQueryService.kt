package com.dh.admin.domain.user.service

import com.dh.admin.application.dto.UserListResponse
import com.dh.admin.domain.user.repository.AdminUserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserQueryService(
    private val adminUserRepository: AdminUserRepository
) {

    fun findUsers(
        keyword: String?,
        roleCode: String?,
        isActive: Boolean?,
        page: Int,
        size: Int
    ): UserListResponse {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(safePage, safeSize)

        val result = adminUserRepository.findUserList(
            keyword = keyword,
            roleCode = roleCode,
            isActive = isActive,
            pageable = pageable
        )

        return UserListResponse(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }
}

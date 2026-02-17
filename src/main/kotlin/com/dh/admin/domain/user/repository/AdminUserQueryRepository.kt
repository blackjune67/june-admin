package com.dh.admin.domain.user.repository

import com.dh.admin.application.dto.UserListItemResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminUserQueryRepository {
    fun findUserList(
        keyword: String?,
        roleCode: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<UserListItemResponse>
}

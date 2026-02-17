package com.dh.admin.application.dto

data class UserListItemResponse(
    val id: Long,
    val email: String,
    val name: String,
    val isActive: Boolean,
    val roles: List<RoleSummary>
)

data class UserListResponse(
    val content: List<UserListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

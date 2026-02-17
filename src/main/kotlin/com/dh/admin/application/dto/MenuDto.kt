package com.dh.admin.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateMenuRequest(
    @field:NotBlank(message = "메뉴 이름은 필수입니다.")
    @field:Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
    val name: String,

    @field:NotBlank(message = "메뉴 코드는 필수입니다.")
    @field:Size(max = 50, message = "메뉴 코드는 50자 이하여야 합니다.")
    val code: String,

    val path: String? = null,
    val icon: String? = null,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val permissionId: Long? = null
)

data class UpdateMenuRequest(
    @field:NotBlank(message = "메뉴 이름은 필수입니다.")
    @field:Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
    val name: String,

    val path: String? = null,
    val icon: String? = null,
    val parentId: Long? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean? = null,
    val permissionId: Long? = null
)

data class MenuResponse(
    val id: Long,
    val name: String,
    val code: String,
    val path: String?,
    val icon: String?,
    val parentId: Long?,
    val sortOrder: Int,
    val isActive: Boolean,
    val requiredPermission: PermissionResponse?,
    val children: List<MenuResponse> = emptyList()
)

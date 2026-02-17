package com.dh.admin.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateRoleRequest(
    @field:NotBlank(message = "역할 코드는 필수입니다.")
    @field:Size(max = 50, message = "역할 코드는 50자 이하여야 합니다.")
    val code: String,

    @field:NotBlank(message = "역할 이름은 필수입니다.")
    @field:Size(max = 100, message = "역할 이름은 100자 이하여야 합니다.")
    val name: String,

    val description: String? = null
)

data class UpdateRoleRequest(
    @field:NotBlank(message = "역할 이름은 필수입니다.")
    @field:Size(max = 100, message = "역할 이름은 100자 이하여야 합니다.")
    val name: String,

    val description: String? = null,
    val isActive: Boolean? = null
)

data class AssignPermissionsRequest(
    val permissionIds: List<Long>
)

data class RoleResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val isSystem: Boolean,
    val permissions: List<PermissionResponse> = emptyList()
)

data class RoleSummary(
    val id: Long,
    val code: String,
    val name: String
)

data class AssignRolesRequest(
    val roleIds: List<Long>
)

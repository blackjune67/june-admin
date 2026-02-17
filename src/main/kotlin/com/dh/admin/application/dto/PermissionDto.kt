package com.dh.admin.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreatePermissionRequest(
    @field:NotBlank(message = "리소스는 필수입니다.")
    @field:Size(max = 50, message = "리소스는 50자 이하여야 합니다.")
    val resource: String,

    @field:NotBlank(message = "액션은 필수입니다.")
    @field:Size(max = 50, message = "액션은 50자 이하여야 합니다.")
    val action: String,

    @field:NotBlank(message = "권한 이름은 필수입니다.")
    @field:Size(max = 100, message = "권한 이름은 100자 이하여야 합니다.")
    val name: String,

    val description: String? = null
)

data class UpdatePermissionRequest(
    @field:NotBlank(message = "권한 이름은 필수입니다.")
    @field:Size(max = 100, message = "권한 이름은 100자 이하여야 합니다.")
    val name: String,

    val description: String? = null
)

data class PermissionResponse(
    val id: Long,
    val resource: String,
    val action: String,
    val name: String,
    val description: String?,
    val authority: String
)

data class PermissionGroupResponse(
    val resource: String,
    val permissions: List<PermissionResponse>
)

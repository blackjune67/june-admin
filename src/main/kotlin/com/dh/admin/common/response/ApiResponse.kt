package com.dh.admin.common.response

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> ok(data: T, message: String? = null) =
            ApiResponse(success = true, data = data, message = message)

        fun ok(message: String? = null) =
            ApiResponse(success = true, data = null, message = message)

        fun error(errorCode: String, message: String) =
            ErrorResponse(errorCode = errorCode, message = message)
    }
}

data class ErrorResponse(
    val success: Boolean = false,
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

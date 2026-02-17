package com.dh.admin.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val status: HttpStatus,
    val title: String
) {
    VALIDATION_ERROR("EFC9400", HttpStatus.BAD_REQUEST, "Validation Error"),
    UNAUTHORIZED("EFC9457", HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN("EFC9403", HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND("EFC9404", HttpStatus.NOT_FOUND, "Not Found"),
    DUPLICATE("EFC9409", HttpStatus.CONFLICT, "Conflict"),
    INTERNAL_SERVER_ERROR("EFC9500", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error")
}

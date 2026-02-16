package com.dh.admin.interfaces.advice

import com.dh.admin.common.exception.*
import com.dh.admin.common.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.BAD_REQUEST, e)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(e: UnauthorizedException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.UNAUTHORIZED, e)

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(e: ForbiddenException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.FORBIDDEN, e)

    @ExceptionHandler(DuplicateException::class)
    fun handleDuplicate(e: DuplicateException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.CONFLICT, e)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult
            .fieldErrors
            .joinToString(", ") { formatFieldError(it) }
            .ifBlank { "요청값이 올바르지 않습니다." }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(errorCode = "VALIDATION_ERROR", message = message)
        )
    }

    private fun buildErrorResponse(status: HttpStatus, e: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(errorCode = e.errorCode, message = e.message)
        )

    private fun formatFieldError(fieldError: FieldError): String =
        "${fieldError.field}: ${fieldError.defaultMessage ?: "유효하지 않은 값입니다."}"
}

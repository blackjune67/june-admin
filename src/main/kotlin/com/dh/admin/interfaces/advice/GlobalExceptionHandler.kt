package com.dh.admin.interfaces.advice

import com.dh.admin.common.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> =
        buildProblemDetail(e.errorCode, e.message, request.requestURI)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val message = e.bindingResult
            .fieldErrors
            .joinToString(", ") { formatFieldError(it) }
            .ifBlank { "요청값이 올바르지 않습니다." }

        return buildProblemDetail(ErrorCode.VALIDATION_ERROR, message, request.requestURI)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> =
        buildProblemDetail(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", request.requestURI)

    private fun buildProblemDetail(
        errorCode: ErrorCode,
        detail: String,
        instancePath: String
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status, detail).apply {
            title = errorCode.title
            type = URI.create("https://api.dh-admin.dev/problems/${errorCode.code}")
            instance = URI.create(instancePath)
            setProperty("errorCode", errorCode.code)
            setProperty("timestamp", OffsetDateTime.now().toString())
        }
        return ResponseEntity.status(errorCode.status).body(problemDetail)
    }

    private fun formatFieldError(fieldError: FieldError): String =
        "${fieldError.field}: ${fieldError.defaultMessage ?: "유효하지 않은 값입니다."}"
}

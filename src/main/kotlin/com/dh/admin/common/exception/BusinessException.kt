package com.dh.admin.common.exception

sealed class BusinessException(
    val errorCode: String,
    override val message: String
) : RuntimeException(message)

class ResourceNotFoundException(message: String) :
    BusinessException("NOT_FOUND", message)

class ValidationException(message: String) :
    BusinessException("VALIDATION_ERROR", message)

class UnauthorizedException(message: String) :
    BusinessException("UNAUTHORIZED", message)

class ForbiddenException(message: String) :
    BusinessException("FORBIDDEN", message)

class DuplicateException(message: String) :
    BusinessException("DUPLICATE", message)

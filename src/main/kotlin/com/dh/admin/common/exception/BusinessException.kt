package com.dh.admin.common.exception

sealed class BusinessException(
    val errorCode: ErrorCode,
    override val message: String
) : RuntimeException(message)

class ResourceNotFoundException(message: String) :
    BusinessException(ErrorCode.NOT_FOUND, message)

class ValidationException(message: String) :
    BusinessException(ErrorCode.VALIDATION_ERROR, message)

class UnauthorizedException(message: String) :
    BusinessException(ErrorCode.UNAUTHORIZED, message)

class ForbiddenException(message: String) :
    BusinessException(ErrorCode.FORBIDDEN, message)

class DuplicateException(message: String) :
    BusinessException(ErrorCode.DUPLICATE, message)

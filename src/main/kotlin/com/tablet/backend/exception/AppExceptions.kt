package com.tablet.backend.exception

sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class ResourceNotFoundException(
    resourceName: String,
    identifier: Any,
) : AppException("$resourceName 을(를) 찾을 수 없습니다. (id: $identifier)")

class DuplicateResourceException(
    message: String,
) : AppException(message)

class InvalidRequestException(
    message: String,
) : AppException(message)

class UnauthorizedException(
    message: String = "인증이 필요합니다.",
) : AppException(message)

class ForbiddenException(
    message: String = "접근 권한이 없습니다.",
) : AppException(message)

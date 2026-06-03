package com.nhan.social.exception

abstract class AppException(
    val errorCode: String,
    val errorMessage: String,
    val httpStatus: Int,
    cause: Throwable? = null,
) : RuntimeException(errorMessage, cause)

// ──────────────────────────────────────────────────────────────────────────────
// Concrete exceptions — default code uses ServiceCode.SYSTEM ("00")
// Override errorCode at call-site for service-specific codes:
//   throw NotFoundException("Article not found", errorCode("03", "0001"))
// ──────────────────────────────────────────────────────────────────────────────

class NotFoundException(
    errorMessage: String,
    errorCode: String = errorCode(ServiceCode.SYSTEM, ErrorType.NOT_FOUND),
) : AppException(errorCode, errorMessage, 404)

class ConflictException(
    errorMessage: String,
    errorCode: String = errorCode(ServiceCode.SYSTEM, ErrorType.CONFLICT),
) : AppException(errorCode, errorMessage, 409)

class UnauthorizedException(
    errorMessage: String = "Unauthorized",
    errorCode: String = errorCode(ServiceCode.SYSTEM, ErrorType.UNAUTHORIZED),
) : AppException(errorCode, errorMessage, 401)

class ForbiddenException(
    errorMessage: String = "Forbidden",
    errorCode: String = errorCode(ServiceCode.SYSTEM, ErrorType.FORBIDDEN),
) : AppException(errorCode, errorMessage, 403)

class BadRequestException(
    errorMessage: String,
    errorCode: String = errorCode(ServiceCode.SYSTEM, ErrorType.BAD_REQUEST),
) : AppException(errorCode, errorMessage, 400)

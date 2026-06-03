package com.nhan.social.exception

abstract class AppException(
    val errorCode: String,      // 4-digit type code, e.g. "0001"
    val errorMessage: String,
    val httpStatus: Int,
    cause: Throwable? = null,
) : RuntimeException(errorMessage, cause)

class NotFoundException(
    errorMessage: String,
    errorCode: String = ErrorType.NOT_FOUND,
) : AppException(errorCode, errorMessage, 404)

class ConflictException(
    errorMessage: String,
    errorCode: String = ErrorType.CONFLICT,
) : AppException(errorCode, errorMessage, 409)

class UnauthorizedException(
    errorMessage: String = "Unauthorized",
    errorCode: String = ErrorType.UNAUTHORIZED,
) : AppException(errorCode, errorMessage, 401)

class ForbiddenException(
    errorMessage: String = "Forbidden",
    errorCode: String = ErrorType.FORBIDDEN,
) : AppException(errorCode, errorMessage, 403)

class BadRequestException(
    errorMessage: String,
    errorCode: String = ErrorType.BAD_REQUEST,
) : AppException(errorCode, errorMessage, 400)

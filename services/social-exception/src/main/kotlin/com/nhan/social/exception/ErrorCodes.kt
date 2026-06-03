package com.nhan.social.exception

/** Four-digit error type codes. Service code prefix injected from application.properties. */
object ErrorType {
    const val NOT_FOUND        = "0001"
    const val CONFLICT         = "0002"
    const val UNAUTHORIZED     = "0003"
    const val FORBIDDEN        = "0004"
    const val BAD_REQUEST      = "0005"
    const val VALIDATION_ERROR = "0006"
    const val INTERNAL_ERROR   = "9999"
}

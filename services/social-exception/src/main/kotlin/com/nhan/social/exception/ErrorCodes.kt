package com.nhan.social.exception

/** Two-digit service identifiers. */
object ServiceCode {
    const val SYSTEM       = "00"   // generic / cross-service
    const val AUTH         = "01"
    const val USER         = "02"
    const val POST         = "03"
    const val INTERACTION  = "04"
    const val NOTIFICATION = "05"
}

/** Four-digit error type codes — append after service code: "01-0001". */
object ErrorType {
    const val NOT_FOUND        = "0001"
    const val CONFLICT         = "0002"
    const val UNAUTHORIZED     = "0003"
    const val FORBIDDEN        = "0004"
    const val BAD_REQUEST      = "0005"
    const val VALIDATION_ERROR = "0006"
    const val INTERNAL_ERROR   = "9999"
}

fun errorCode(service: String, type: String) = "$service-$type"

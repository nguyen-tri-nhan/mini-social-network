package com.nhan.social.common.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)
        fun ok() = ApiResponse<Unit>(success = true)
        fun error(error: ErrorResponse) = ApiResponse<Unit>(success = false, error = error)
    }
}

data class ErrorResponse(
    val errorCode: String,
    val errorMessage: String,
    val traceId: String,
)

data class PageResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

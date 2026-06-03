package com.nhan.social.exception.mapper

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.common.dto.ErrorResponse
import com.nhan.social.exception.ErrorType
import com.nhan.social.exception.ServiceCode
import com.nhan.social.exception.errorCode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
@ApplicationScoped
class ValidationExceptionMapper : ExceptionMapper<ConstraintViolationException> {

    override fun toResponse(exception: ConstraintViolationException): Response {
        val message = exception.constraintViolations
            .joinToString("; ") { "${it.propertyPath}: ${it.message}" }

        return Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(
                ApiResponse.error(
                    ErrorResponse(
                        errorCode = errorCode(ServiceCode.SYSTEM, ErrorType.VALIDATION_ERROR),
                        errorMessage = message,
                        traceId = currentTraceId(),
                    )
                )
            )
            .build()
    }
}

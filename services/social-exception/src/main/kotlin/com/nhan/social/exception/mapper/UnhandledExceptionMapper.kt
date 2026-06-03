package com.nhan.social.exception.mapper

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.common.dto.ErrorResponse
import com.nhan.social.exception.ErrorType
import com.nhan.social.exception.ServiceCode
import com.nhan.social.exception.errorCode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

@Provider
@ApplicationScoped
class UnhandledExceptionMapper : ExceptionMapper<Exception> {

    private val log = Logger.getLogger(UnhandledExceptionMapper::class.java)

    override fun toResponse(exception: Exception): Response {
        if (exception is WebApplicationException) return exception.response

        val traceId = currentTraceId()
        log.errorf(exception, "Unhandled exception (traceId=%s)", traceId)

        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(
                ApiResponse.error(
                    ErrorResponse(
                        errorCode = errorCode(ServiceCode.SYSTEM, ErrorType.INTERNAL_ERROR),
                        errorMessage = "An unexpected error occurred",
                        traceId = traceId,
                    )
                )
            )
            .build()
    }
}

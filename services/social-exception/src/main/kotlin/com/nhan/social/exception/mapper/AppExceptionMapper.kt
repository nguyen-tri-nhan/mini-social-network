package com.nhan.social.exception.mapper

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.common.dto.ErrorResponse
import com.nhan.social.exception.AppException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@Provider
@ApplicationScoped
class AppExceptionMapper : ExceptionMapper<AppException> {

    @ConfigProperty(name = "app.service-code")
    lateinit var serviceCode: String

    private val log = Logger.getLogger(AppExceptionMapper::class.java)

    override fun toResponse(exception: AppException): Response {
        val traceId = currentTraceId()
        val fullCode = "$serviceCode-${exception.errorCode}"
        log.debugf("[%s] HTTP %d — %s (traceId=%s)", fullCode, exception.httpStatus, exception.errorMessage, traceId)
        return Response
            .status(exception.httpStatus)
            .type(MediaType.APPLICATION_JSON)
            .entity(ApiResponse.error(ErrorResponse(fullCode, exception.errorMessage, traceId)))
            .build()
    }
}

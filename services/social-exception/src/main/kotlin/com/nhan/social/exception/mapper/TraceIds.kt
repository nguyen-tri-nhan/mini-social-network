package com.nhan.social.exception.mapper

import io.opentelemetry.api.trace.Span
import java.util.UUID

/** Returns the current OTel trace ID, or a random UUID if OTel is not active. */
fun currentTraceId(): String {
    val spanCtx = Span.current().spanContext
    return if (spanCtx.isValid) spanCtx.traceId
    else UUID.randomUUID().toString()
}

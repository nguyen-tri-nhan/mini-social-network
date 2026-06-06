package com.nhan.social.exception.filter

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty

// Guards /internal/** — only services with X-Service-Secret-Key header can call these endpoints.
// Config: app.internal.secret-key (env: INTERNAL_SECRET_KEY, default: dev-internal-secret)
@Provider
@Priority(Priorities.AUTHENTICATION)
class InternalAuthFilter : ContainerRequestFilter {

    @ConfigProperty(name = "app.internal.secret-key", defaultValue = "dev-internal-secret")
    lateinit var secretKey: String

    override fun filter(ctx: ContainerRequestContext) {
        if (!ctx.uriInfo.path.startsWith("internal")) return

        val key = ctx.getHeaderString("X-Service-Secret-Key")
        if (key != secretKey) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }
}

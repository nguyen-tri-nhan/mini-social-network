package com.nhan.social.notification.resource

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.notification.service.NotificationService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class NotificationResource(
    private val service: NotificationService,
    private val jwt: JsonWebToken,
) {
    private val currentUserId: UUID get() = UUID.fromString(jwt.subject)

    @GET
    @RolesAllowed("ROLE_USER")
    fun list(
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int,
    ): Response = Response.ok(ApiResponse.ok(service.list(currentUserId, page, size.coerceAtMost(50)))).build()

    @GET
    @Path("/unread-count")
    @RolesAllowed("ROLE_USER")
    fun unreadCount(): Response = Response.ok(ApiResponse.ok(service.getUnreadCount(currentUserId))).build()

    @PATCH
    @Path("/{id}/seen")
    @RolesAllowed("ROLE_USER")
    fun markSeen(@PathParam("id") id: UUID): Response {
        service.markSeen(id, currentUserId)
        return Response.noContent().build()
    }

    @PATCH
    @Path("/seen-all")
    @RolesAllowed("ROLE_USER")
    fun markAllSeen(): Response {
        service.markAllSeen(currentUserId)
        return Response.noContent().build()
    }
}

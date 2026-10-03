package com.nhan.social.chat.resource

import com.nhan.social.chat.dto.CreateConversationRequest
import com.nhan.social.chat.dto.MarkReadRequest
import com.nhan.social.chat.dto.SendMessageRequest
import com.nhan.social.chat.dto.UnreadCountDto
import com.nhan.social.chat.service.ChatService
import com.nhan.social.common.dto.ApiResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ROLE_USER")
class ConversationResource(
    private val service: ChatService,
    private val jwt: JsonWebToken,
) {
    private val currentUserId: UUID get() = UUID.fromString(jwt.subject)

    @POST
    fun open(@Valid request: CreateConversationRequest): Response =
        Response.ok(ApiResponse.ok(service.openDirect(currentUserId, request.targetUserId!!))).build()

    @GET
    fun list(
        @QueryParam("before") before: UUID?,
        @QueryParam("size") @DefaultValue("20") @Min(1) size: Int,
    ): Response = Response.ok(ApiResponse.ok(service.list(currentUserId, before, size.coerceAtMost(50)))).build()

    @GET
    @Path("/unread-count")
    fun unreadCount(): Response =
        Response.ok(ApiResponse.ok(UnreadCountDto(service.unreadCount(currentUserId)))).build()

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: UUID): Response =
        Response.ok(ApiResponse.ok(service.get(currentUserId, id))).build()

    @POST
    @Path("/{id}/read")
    fun markRead(@PathParam("id") id: UUID, @Valid request: MarkReadRequest): Response {
        service.markRead(currentUserId, id, request.messageId!!)
        return Response.noContent().build()
    }

    @GET
    @Path("/{id}/messages")
    fun messages(
        @PathParam("id") id: UUID,
        @QueryParam("before") before: UUID?,
        @QueryParam("size") @DefaultValue("30") @Min(1) size: Int,
    ): Response = Response.ok(ApiResponse.ok(service.messages(currentUserId, id, before, size.coerceAtMost(100)))).build()

    @POST
    @Path("/{id}/messages")
    fun send(@PathParam("id") id: UUID, @Valid request: SendMessageRequest): Response =
        Response.status(Response.Status.CREATED)
            .entity(ApiResponse.ok(service.send(currentUserId, id, request.clientMessageId!!, request.content)))
            .build()
}

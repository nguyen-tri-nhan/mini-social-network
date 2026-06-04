package com.nhan.social.interaction.resource

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.interaction.dto.CastVoteRequest
import com.nhan.social.interaction.dto.CreateCommentRequest
import com.nhan.social.interaction.service.InteractionService
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InteractionResource(
    private val service: InteractionService,
    private val jwt: JsonWebToken,
) {
    private val currentUserId: UUID get() = UUID.fromString(jwt.subject)

    // ── Comments ──────────────────────────────────────────────────────────────

    @GET
    @Path("/comments")
    @RolesAllowed("ROLE_USER")
    fun listComments(
        @QueryParam("targetId")   targetId: UUID,
        @QueryParam("targetType") targetType: String,
        @QueryParam("page") @DefaultValue("0")  page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int,
    ): Response = Response.ok(ApiResponse.ok(
        service.listComments(targetId, targetType.uppercase(), page, size.coerceAtMost(50))
    )).build()

    @POST
    @Path("/comments")
    @RolesAllowed("ROLE_USER")
    fun addComment(@Valid request: CreateCommentRequest): Response =
        Response.status(Response.Status.CREATED)
            .entity(ApiResponse.ok(service.addComment(currentUserId, request)))
            .build()

    @DELETE
    @Path("/comments/{id}")
    @RolesAllowed("ROLE_USER")
    fun deleteComment(@PathParam("id") id: UUID): Response {
        service.deleteComment(id, currentUserId)
        return Response.noContent().build()
    }

    // ── Votes ─────────────────────────────────────────────────────────────────

    @POST
    @Path("/votes")
    @RolesAllowed("ROLE_USER")
    fun castVote(@Valid request: CastVoteRequest): Response =
        Response.ok(ApiResponse.ok(service.castVote(currentUserId, request))).build()
}

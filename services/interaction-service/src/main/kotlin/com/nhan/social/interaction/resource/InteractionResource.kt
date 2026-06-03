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

    @GET
    @Path("/articles/{articleId}/comments")
    @RolesAllowed("ROLE_USER")
    fun listComments(
        @PathParam("articleId") articleId: UUID,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int,
    ): Response {
        val result = service.listComments(articleId, page, size.coerceAtMost(50))
        return Response.ok(ApiResponse.ok(result)).build()
    }

    @POST
    @Path("/articles/{articleId}/comments")
    @RolesAllowed("ROLE_USER")
    fun addComment(
        @PathParam("articleId") articleId: UUID,
        @Valid request: CreateCommentRequest,
    ): Response {
        val comment = service.addComment(articleId, currentUserId, request)
        return Response.status(Response.Status.CREATED).entity(ApiResponse.ok(comment)).build()
    }

    @DELETE
    @Path("/comments/{commentId}")
    @RolesAllowed("ROLE_USER")
    fun deleteComment(@PathParam("commentId") commentId: UUID): Response {
        service.deleteComment(commentId, currentUserId)
        return Response.noContent().build()
    }

    @POST
    @Path("/articles/{targetId}/vote")
    @RolesAllowed("ROLE_USER")
    fun voteArticle(
        @PathParam("targetId") targetId: UUID,
        @Valid request: CastVoteRequest,
    ): Response {
        val vote = service.castVote(targetId, "ARTICLE", currentUserId, request)
        return Response.ok(ApiResponse.ok(vote)).build()
    }

    @POST
    @Path("/comments/{targetId}/vote")
    @RolesAllowed("ROLE_USER")
    fun voteComment(
        @PathParam("targetId") targetId: UUID,
        @Valid request: CastVoteRequest,
    ): Response {
        val vote = service.castVote(targetId, "COMMENT", currentUserId, request)
        return Response.ok(ApiResponse.ok(vote)).build()
    }
}

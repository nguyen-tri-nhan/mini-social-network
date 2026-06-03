package com.nhan.social.post.resource

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.post.dto.CreateArticleRequest
import com.nhan.social.post.service.ArticleService
import com.nhan.social.post.service.S3Service
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ArticleResource(
    private val articleService: ArticleService,
    private val s3Service: S3Service,
    private val jwt: JsonWebToken,
) {
    private val currentUserId: UUID get() = UUID.fromString(jwt.subject)

    @GET
    @RolesAllowed("ROLE_USER")
    fun list(@BeanParam params: ArticleListParams): Response =
        Response.ok(ApiResponse.ok(articleService.listArticles(params.toQuery()))).build()

    @GET
    @Path("/{id}")
    @RolesAllowed("ROLE_USER")
    fun getById(@PathParam("id") id: UUID): Response =
        Response.ok(ApiResponse.ok(articleService.getById(id))).build()

    @POST
    @RolesAllowed("ROLE_USER")
    fun create(@Valid request: CreateArticleRequest): Response =
        Response.status(Response.Status.CREATED)
            .entity(ApiResponse.ok(articleService.create(currentUserId, request)))
            .build()

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_USER")
    fun delete(@PathParam("id") id: UUID): Response {
        articleService.delete(id, currentUserId)
        return Response.noContent().build()
    }

    @POST
    @Path("/images/presign")
    @RolesAllowed("ROLE_USER")
    fun presignUpload(
        @QueryParam("filename") filename: String,
        @QueryParam("contentType") @DefaultValue("image/jpeg") contentType: String,
    ): Response = Response.ok(ApiResponse.ok(s3Service.presignUpload(filename, contentType))).build()
}

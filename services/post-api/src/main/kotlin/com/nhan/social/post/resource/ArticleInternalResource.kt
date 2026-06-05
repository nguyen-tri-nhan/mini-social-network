package com.nhan.social.post.resource

import com.nhan.social.post.repository.ArticleRepository
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/internal/articles")
@Produces(MediaType.APPLICATION_JSON)
class ArticleInternalResource(private val repo: ArticleRepository) {

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: UUID): Response =
        repo.findById(id)
            ?.let { Response.ok(mapOf("authorId" to it.authorId.toString())).build() }
            ?: Response.status(Response.Status.NOT_FOUND).build()
}

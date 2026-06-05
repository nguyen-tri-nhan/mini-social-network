package com.nhan.social.interaction.client

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

data class ArticleRef(val authorId: String)

@RegisterRestClient(configKey = "post-api")
@Path("/internal/articles")
@Produces(MediaType.APPLICATION_JSON)
interface PostApiClient {

    @GET
    @Path("/{id}")
    fun getArticle(@PathParam("id") id: UUID): ArticleRef
}

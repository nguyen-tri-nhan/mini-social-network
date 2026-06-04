package com.nhan.social.auth.resource

import com.nhan.social.auth.security.JwtService
import com.nhan.social.common.dto.ApiResponse
import io.quarkus.arc.profile.IfBuildProfile
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/dev")
@Produces(MediaType.APPLICATION_JSON)
@IfBuildProfile("dev")
class DevTokenResource(private val jwtService: JwtService) {

    @GET
    @Path("/token")
    fun token(
        @QueryParam("userId")   @DefaultValue("00000000-0000-0000-0000-000000000001") userId: String,
        @QueryParam("username") @DefaultValue("dev-user") username: String,
        @QueryParam("role")     @DefaultValue("ROLE_USER") role: String,
    ): Response {
        val token = jwtService.generateToken(userId, username, setOf(role), expiryDays = 365)
        return Response.ok(ApiResponse.ok(mapOf(
            "token"    to token,
            "userId"   to userId,
            "username" to username,
            "usage"    to "Authorization: Bearer $token",
        ))).build()
    }
}

package com.nhan.social.user.resource

import com.nhan.social.common.dto.ApiResponse
import com.nhan.social.user.dto.UpdateProfileRequest
import com.nhan.social.user.service.UserService
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource(
    private val userService: UserService,
    private val jwt: JsonWebToken,
) {
    private val currentUserId: UUID get() = UUID.fromString(jwt.subject)

    @GET
    @Path("/me")
    @RolesAllowed("ROLE_USER")
    fun getMe(): Response {
        val profile = userService.getById(currentUserId)
        return Response.ok(ApiResponse.ok(profile)).build()
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ROLE_USER")
    fun getById(@PathParam("id") id: UUID): Response {
        val profile = userService.getCachedById(id)
        return Response.ok(ApiResponse.ok(profile)).build()
    }

    @PATCH
    @Path("/me")
    @RolesAllowed("ROLE_USER")
    fun updateMe(@Valid request: UpdateProfileRequest): Response {
        val updated = userService.update(currentUserId, request)
        return Response.ok(ApiResponse.ok(updated)).build()
    }
}

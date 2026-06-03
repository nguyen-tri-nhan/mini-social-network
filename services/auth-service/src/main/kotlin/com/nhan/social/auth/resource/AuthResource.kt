package com.nhan.social.auth.resource

import com.nhan.social.auth.dto.SignInRequest
import com.nhan.social.auth.dto.SignUpRequest
import com.nhan.social.auth.service.AuthService
import com.nhan.social.common.dto.ApiResponse
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AuthResource(private val authService: AuthService) {

    @POST
    @Path("/signup")
    fun signup(@Valid request: SignUpRequest): Response {
        val result = authService.signup(request)
        return Response.status(Response.Status.CREATED)
            .entity(ApiResponse.ok(result))
            .build()
    }

    @POST
    @Path("/signin")
    fun signin(@Valid request: SignInRequest): Response {
        val result = authService.signin(request)
        return Response.ok(ApiResponse.ok(result)).build()
    }
}

package com.nhan.social.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank val username: String = "",
    @field:NotBlank @field:Email val email: String = "",
    @field:NotBlank @field:Size(min = 6) val password: String = "",
    @field:NotBlank val firstname: String = "",
    @field:NotBlank val lastname: String = "",
)

data class SignInRequest(
    @field:NotBlank val identifier: String = "",
    @field:NotBlank val password: String = "",
)

data class AuthResponse(
    val accessToken: String,
    val userId: String,
    val username: String,
)

package com.nhan.social.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// Giới hạn độ dài phải khớp cột user_profile (user-service) — firstname/lastname
// không lưu ở auth nên nếu vượt quá, signup vẫn 201 nhưng user-consumer ghi
// profile thất bại async → tài khoản hỏng vĩnh viễn (/me luôn 404).
data class SignUpRequest(
    @field:NotBlank @field:Size(max = 50) val username: String = "",
    @field:NotBlank @field:Email @field:Size(max = 255) val email: String = "",
    @field:NotBlank @field:Size(min = 6) val password: String = "",
    @field:NotBlank @field:Size(max = 100) val firstname: String = "",
    @field:NotBlank @field:Size(max = 100) val lastname: String = "",
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

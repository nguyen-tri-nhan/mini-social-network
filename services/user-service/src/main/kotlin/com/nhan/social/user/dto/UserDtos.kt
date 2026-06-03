package com.nhan.social.user.dto

import com.nhan.social.user.entity.UserProfile
import jakarta.validation.constraints.Size
import java.time.Instant

data class UserProfileDto(
    val id: String,
    val username: String,
    val email: String,
    val firstname: String,
    val lastname: String,
    val avatarUrl: String?,
    val createdAt: Instant,
)

data class UpdateProfileRequest(
    @field:Size(max = 100) val firstname: String? = null,
    @field:Size(max = 100) val lastname: String? = null,
    val avatarUrl: String? = null,
)

fun UserProfile.toDto() = UserProfileDto(
    id = id.toString(),
    username = username,
    email = email,
    firstname = firstname,
    lastname = lastname,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)

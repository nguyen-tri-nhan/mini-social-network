package com.nhan.social.common.dto

import java.time.Instant

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val firstname: String,
    val lastname: String,
    val avatarUrl: String?,
    val createdAt: Instant,
)

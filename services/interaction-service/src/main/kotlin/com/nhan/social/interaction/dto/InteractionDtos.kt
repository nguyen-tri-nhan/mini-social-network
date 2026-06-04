package com.nhan.social.interaction.dto

import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.Vote
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCommentRequest(
    @field:NotNull val targetId: UUID = UUID(0, 0),
    @field:NotBlank val targetType: String = "",   // "ARTICLE" | "COMMENT"
    @field:NotBlank @field:Size(max = 1000) val description: String = "",
)

data class CastVoteRequest(
    @field:NotNull val targetId: UUID = UUID(0, 0),
    @field:NotBlank val targetType: String = "",   // "ARTICLE" | "COMMENT"
    @field:Min(-1) @field:Max(1) val value: Int = 0,
)

data class CommentDto(
    val id: String,
    val description: String,
    val targetId: String,
    val targetType: String,
    val authorId: String,
    val createdAt: Instant,
)

data class VoteDto(
    val id: String,
    val value: Int,
    val userId: String,
    val targetId: String,
    val targetType: String,
)

fun Comment.toDto() = CommentDto(
    id          = id.toString(),
    description = description,
    targetId    = targetId.toString(),
    targetType  = targetType,
    authorId    = authorId.toString(),
    createdAt   = createdAt,
)

fun Vote.toDto() = VoteDto(
    id         = id.toString(),
    value      = value.toInt(),
    userId     = userId.toString(),
    targetId   = targetId.toString(),
    targetType = targetType,
)

package com.nhan.social.post.dto

import com.nhan.social.post.entity.Article
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateArticleRequest(
    @field:Size(max = 2000) val description: String? = null,
    val imageUrl: String? = null,
) {
    init {
        require(!description.isNullOrBlank() || !imageUrl.isNullOrBlank()) {
            "Article must have either description or image"
        }
    }
}

data class ArticleDto(
    val id: String,
    val description: String?,
    val imageUrl: String?,
    val authorId: String,
    val voteCount: Int,
    val commentCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PresignResponse(val uploadUrl: String, val imageUrl: String)

fun Article.toDto() = ArticleDto(
    id = id.toString(),
    description = description,
    imageUrl = imageUrl,
    authorId = authorId.toString(),
    voteCount = voteCount,
    commentCount = commentCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

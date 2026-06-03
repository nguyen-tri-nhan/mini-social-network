package com.nhan.social.common.event

import java.time.Instant

enum class EventType {
    ARTICLE_CREATED,
    COMMENT_CREATED,
    VOTE_CAST,
    USER_UPDATED,
}

data class SocialEvent(
    val eventType: EventType,
    val occurredAt: Instant = Instant.now(),
    val payload: Map<String, String>,
)

fun articleCreatedEvent(articleId: String, authorId: String) = SocialEvent(
    eventType = EventType.ARTICLE_CREATED,
    payload = mapOf("articleId" to articleId, "authorId" to authorId),
)

fun commentCreatedEvent(
    commentId: String,
    articleId: String,
    actorId: String,
    articleAuthorId: String,
) = SocialEvent(
    eventType = EventType.COMMENT_CREATED,
    payload = mapOf(
        "commentId" to commentId,
        "articleId" to articleId,
        "actorId" to actorId,
        "articleAuthorId" to articleAuthorId,
    ),
)

fun voteCastEvent(
    targetId: String,
    targetType: String,
    actorId: String,
    targetAuthorId: String,
) = SocialEvent(
    eventType = EventType.VOTE_CAST,
    payload = mapOf(
        "targetId" to targetId,
        "targetType" to targetType,
        "actorId" to actorId,
        "targetAuthorId" to targetAuthorId,
    ),
)

fun userUpdatedEvent(userId: String) = SocialEvent(
    eventType = EventType.USER_UPDATED,
    payload = mapOf("userId" to userId),
)

package com.nhan.social.common.event

import java.time.Instant

enum class EventType {
    // social.auth
    USER_CREATED,
    USER_UPDATED,

    // social.post
    ARTICLE_CREATED,
    ARTICLE_DELETED,

    // social.interaction
    COMMENT_CREATED,
    VOTE_CAST,

    // social.chat (Phase 3)
    CHAT_MESSAGE,
}

data class SocialEvent(
    val eventType: EventType,
    val occurredAt: Instant = Instant.now(),
    val payload: Map<String, String>,
    val eventId: String? = null,   // outbox UUID — dùng cho idempotency ở consumer
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

fun userCreatedEvent(userId: String, username: String) = SocialEvent(
    eventType = EventType.USER_CREATED,
    payload = mapOf("userId" to userId, "username" to username),
)

fun userUpdatedEvent(userId: String) = SocialEvent(
    eventType = EventType.USER_UPDATED,
    payload = mapOf("userId" to userId),
)

fun articleDeletedEvent(articleId: String, authorId: String) = SocialEvent(
    eventType = EventType.ARTICLE_DELETED,
    payload = mapOf("articleId" to articleId, "authorId" to authorId),
)

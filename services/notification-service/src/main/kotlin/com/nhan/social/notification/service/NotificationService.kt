package com.nhan.social.notification.service

import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ErrorType
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.exception.ServiceCode
import com.nhan.social.exception.errorCode
import com.nhan.social.notification.entity.Notification
import com.nhan.social.notification.repository.NotificationRepository
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

data class NotificationDto(
    val id: String,
    val type: String,
    val actorId: String,
    val ownerId: String,
    val articleId: String?,
    val seen: Boolean,
    val createdAt: Instant,
)

data class UnreadCountDto(val count: Long)

fun Notification.toDto() = NotificationDto(
    id = id.toString(),
    type = type,
    actorId = actorId.toString(),
    ownerId = ownerId.toString(),
    articleId = articleId?.toString(),
    seen = seen,
    createdAt = createdAt,
)

@ApplicationScoped
class NotificationService(
    private val repo: NotificationRepository,
    private val redis: RedisDataSource,
) {
    private val log = Logger.getLogger(NotificationService::class.java)
    private val valueCommands get() = redis.value(String::class.java)

    fun list(ownerId: UUID, page: Int, size: Int): PageResponse<NotificationDto> {
        val items = repo.findByOwner(ownerId, page, size).map { it.toDto() }
        val total = repo.count("ownerId = ?1", ownerId)
        return PageResponse(items = items, total = total, page = page, size = size, hasNext = (page + 1) * size < total)
    }

    fun getUnreadCount(ownerId: UUID): UnreadCountDto {
        val key = "user:$ownerId:noti_unread"
        val cached = valueCommands.get(key)?.toLongOrNull()
        if (cached != null) return UnreadCountDto(cached)

        val count = repo.countUnread(ownerId)
        valueCommands.set(key, count.toString())
        return UnreadCountDto(count)
    }

    @Transactional
    fun markSeen(id: UUID, requesterId: UUID) {
        val notification = repo.findById(id) ?: throw NotFoundException("Notification not found", errorCode(ServiceCode.NOTIFICATION, ErrorType.NOT_FOUND))
        if (notification.ownerId != requesterId) throw ForbiddenException(errorCode = errorCode(ServiceCode.NOTIFICATION, ErrorType.FORBIDDEN))
        notification.seen = true
        decrementUnread(requesterId)
    }

    @Transactional
    fun markAllSeen(ownerId: UUID) {
        repo.markAllSeen(ownerId)
        valueCommands.set("user:$ownerId:noti_unread", "0")
    }

    @Transactional
    fun createFromEvent(event: SocialEvent) {
        val payload = event.payload
        val actorId = payload["actorId"]?.let { UUID.fromString(it) } ?: return
        val ownerId = when (event.eventType) {
            EventType.COMMENT_CREATED -> payload["articleAuthorId"]
            EventType.VOTE_CAST -> payload["targetAuthorId"]
            else -> null
        }?.takeIf { it.isNotBlank() }?.let { UUID.fromString(it) } ?: return

        if (actorId == ownerId) return

        repo.persist(Notification().apply {
            this.id = UUID.randomUUID()
            this.type = event.eventType.name
            this.actorId = actorId
            this.ownerId = ownerId
            this.articleId = payload["articleId"]?.let { UUID.fromString(it) }
            this.seen = false
            this.createdAt = Instant.now()
        })

        try {
            valueCommands.incr("user:$ownerId:noti_unread")
        } catch (e: Exception) {
            log.errorf(e, "Failed to increment unread count for user %s", ownerId)
        }
    }

    private fun decrementUnread(ownerId: UUID) {
        try {
            valueCommands.decr("user:$ownerId:noti_unread")
        } catch (e: Exception) {
            log.errorf(e, "Failed to decrement unread count for user %s", ownerId)
        }
    }
}

package com.nhan.social.notification.service

import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.notification.entity.Notification
import com.nhan.social.notification.repository.NotificationRepository
import io.mockk.*
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class NotificationServiceTest {

    private val repo          = mockk<NotificationRepository>()
    private val redis         = mockk<RedisDataSource>()
    private val valueCommands = mockk<ValueCommands<String, String>>()
    private val service       = NotificationService(repo, redis)

    init {
        every { redis.value(String::class.java) } returns valueCommands
    }

    private fun notification(ownerId: UUID = UUID.randomUUID(), seen: Boolean = false) =
        Notification().apply {
            this.id        = UUID.randomUUID()
            this.type      = EventType.COMMENT_CREATED.name
            this.actorId   = UUID.randomUUID()
            this.ownerId   = ownerId
            this.seen      = seen
            this.createdAt = Instant.now()
        }

    private fun commentEvent(
        actorId: String  = UUID.randomUUID().toString(),
        ownerId: String  = UUID.randomUUID().toString(),
        articleId: String = UUID.randomUUID().toString(),
        eventId: String? = UUID.randomUUID().toString(),
    ) = SocialEvent(
        eventType = EventType.COMMENT_CREATED,
        payload   = mapOf(
            "actorId"        to actorId,
            "articleAuthorId" to ownerId,
            "articleId"      to articleId,
        ),
        eventId = eventId,
    )

    // ── U-21 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-21 list returns paged notifications for owner`() {
        val ownerId = UUID.randomUUID()
        val items   = listOf(notification(ownerId), notification(ownerId))
        every { repo.findByOwner(ownerId, 0, 20) } returns items
        every { repo.count(any<String>(), ownerId) } returns 2

        val result = service.list(ownerId, page = 0, size = 20)

        assertEquals(2, result.total)
        assertEquals(2, result.items.size)
        assertTrue(result.items.all { it.ownerId == ownerId.toString() })
    }

    // ── U-22 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-22 markSeen own notification - sets seen to true`() {
        val ownerId = UUID.randomUUID()
        val noti    = notification(ownerId = ownerId, seen = false)
        every { repo.findById(noti.id) } returns noti
        every { valueCommands.decr(any()) } returns 0

        service.markSeen(noti.id, requesterId = ownerId)

        assertTrue(noti.seen)
    }

    // ── U-23 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-23 markSeen other's notification - throws ForbiddenException`() {
        val noti = notification(ownerId = UUID.randomUUID())
        every { repo.findById(noti.id) } returns noti

        assertThrows<ForbiddenException> {
            service.markSeen(noti.id, requesterId = UUID.randomUUID())
        }
        assertFalse(noti.seen)
    }

    @Test
    fun `markSeen non-existent notification - throws NotFoundException`() {
        every { repo.findById(any()) } returns null

        assertThrows<NotFoundException> {
            service.markSeen(UUID.randomUUID(), requesterId = UUID.randomUUID())
        }
    }

    // ── U-24 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-24 markAllSeen - repo called and Redis count reset to 0`() {
        val ownerId = UUID.randomUUID()
        every { repo.markAllSeen(ownerId) } just Runs
        every { valueCommands.set(any(), "0") } just Runs

        service.markAllSeen(ownerId)

        verify { repo.markAllSeen(ownerId) }
        verify { valueCommands.set("user:$ownerId:noti_unread", "0") }
    }

    // ── createFromEvent ───────────────────────────────────────────────────────

    @Test
    fun `createFromEvent COMMENT_CREATED - notification persisted with correct fields`() {
        val actorId   = UUID.randomUUID().toString()
        val ownerId   = UUID.randomUUID().toString()
        val articleId = UUID.randomUUID().toString()
        val event     = commentEvent(actorId, ownerId, articleId)

        every { repo.existsByEventId(any()) } returns false
        every { repo.persist(any<Notification>()) } just Runs
        every { valueCommands.incr(any()) } returns 1

        service.createFromEvent(event)

        val slot = slot<Notification>()
        verify { repo.persist(capture(slot)) }
        assertEquals(actorId, slot.captured.actorId.toString())
        assertEquals(ownerId, slot.captured.ownerId.toString())
        assertEquals(articleId, slot.captured.articleId.toString())
        assertFalse(slot.captured.seen)
    }

    @Test
    fun `createFromEvent duplicate eventId - skipped (idempotency)`() {
        val eventId = UUID.randomUUID()
        val event   = commentEvent(eventId = eventId.toString())
        every { repo.existsByEventId(eventId) } returns true

        service.createFromEvent(event)

        verify(exactly = 0) { repo.persist(any<Notification>()) }
    }

    @Test
    fun `createFromEvent actor == owner - no self-notification`() {
        val userId = UUID.randomUUID().toString()
        val event  = commentEvent(actorId = userId, ownerId = userId)
        every { repo.existsByEventId(any()) } returns false

        service.createFromEvent(event)

        verify(exactly = 0) { repo.persist(any<Notification>()) }
    }
}

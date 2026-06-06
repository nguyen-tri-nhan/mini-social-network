package com.nhan.social.interaction.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.interaction.client.ArticleRef
import com.nhan.social.interaction.client.PostApiClient
import com.nhan.social.interaction.dto.CastVoteRequest
import com.nhan.social.interaction.dto.CreateCommentRequest
import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.OutboxEntry
import com.nhan.social.interaction.entity.Vote
import com.nhan.social.interaction.repository.CommentRepository
import com.nhan.social.interaction.repository.OutboxRepository
import com.nhan.social.interaction.repository.VoteRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class InteractionServiceTest {

    private val objectMapper  = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val commentRepo   = mockk<CommentRepository>()
    private val voteRepo      = mockk<VoteRepository>()
    private val outboxRepo    = mockk<OutboxRepository>()
    private val postApiClient = mockk<PostApiClient>()
    private val service       = InteractionService(commentRepo, voteRepo, outboxRepo, objectMapper, postApiClient)

    private fun comment(authorId: UUID = UUID.randomUUID()) = Comment().apply {
        this.id          = UUID.randomUUID()
        this.targetId    = UUID.randomUUID()
        this.targetType  = "ARTICLE"
        this.authorId    = authorId
        this.description = "Nice post!"
        this.visible     = true
        this.createdAt   = Instant.now()
        this.updatedAt   = Instant.now()
    }

    private fun capturedEvent(): SocialEvent {
        val slot = slot<OutboxEntry>()
        verify { outboxRepo.persist(capture(slot)) }
        return objectMapper.readValue(slot.captured.payload, SocialEvent::class.java)
    }

    // ── U-15 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-15 addComment valid - comment persisted and outbox event published`() {
        val authorId  = UUID.randomUUID()
        val articleId = UUID.randomUUID()
        every { commentRepo.persist(any<Comment>()) } just Runs
        every { outboxRepo.persist(any<OutboxEntry>()) } just Runs
        every { postApiClient.getArticle(articleId) } returns ArticleRef("owner-1")

        service.addComment(authorId, CreateCommentRequest(articleId, "ARTICLE", "Nice!"))

        verify { commentRepo.persist(any<Comment>()) }
        val event = capturedEvent()
        assertEquals(EventType.COMMENT_CREATED, event.eventType)
        assertEquals(authorId.toString(), event.payload["actorId"])
        assertEquals("owner-1", event.payload["articleAuthorId"])
        assertEquals(articleId.toString(), event.payload["articleId"])
    }

    // ── U-16 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-16 deleteComment own comment - sets visible to false`() {
        val authorId = UUID.randomUUID()
        val c        = comment(authorId = authorId)
        every { commentRepo.findById(c.id) } returns c

        service.deleteComment(c.id, requesterId = authorId)

        assertFalse(c.visible)
    }

    // ── U-17 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-17 deleteComment other's comment - throws ForbiddenException`() {
        val c = comment(authorId = UUID.randomUUID())
        every { commentRepo.findById(c.id) } returns c

        assertThrows<ForbiddenException> {
            service.deleteComment(c.id, requesterId = UUID.randomUUID())
        }
        assertTrue(c.visible)
    }

    @Test
    fun `deleteComment non-existent - throws NotFoundException`() {
        every { commentRepo.findById(any()) } returns null

        assertThrows<NotFoundException> {
            service.deleteComment(UUID.randomUUID(), UUID.randomUUID())
        }
    }

    // ── U-18 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-18 castVote new vote - vote persisted with delta equal to value`() {
        val userId    = UUID.randomUUID()
        val articleId = UUID.randomUUID()
        every { voteRepo.findByUserAndTarget(userId, articleId, "ARTICLE") } returns null
        every { voteRepo.persist(any<Vote>()) } just Runs
        every { outboxRepo.persist(any<OutboxEntry>()) } just Runs
        every { postApiClient.getArticle(articleId) } returns ArticleRef("owner-1")

        service.castVote(userId, CastVoteRequest(articleId, "ARTICLE", 1))

        val event = capturedEvent()
        assertEquals(EventType.VOTE_CAST, event.eventType)
        assertEquals("1", event.payload["delta"])
        assertEquals(userId.toString(), event.payload["actorId"])
    }

    // ── U-19 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-19 castVote existing vote - vote updated, delta is difference`() {
        val userId    = UUID.randomUUID()
        val articleId = UUID.randomUUID()
        val existing  = Vote().apply {
            id         = UUID.randomUUID()
            this.userId     = userId
            this.targetId   = articleId
            this.targetType = "ARTICLE"
            this.value      = 1   // existing upvote
            this.createdAt  = Instant.now()
        }
        every { voteRepo.findByUserAndTarget(userId, articleId, "ARTICLE") } returns existing
        every { outboxRepo.persist(any<OutboxEntry>()) } just Runs
        every { postApiClient.getArticle(articleId) } returns ArticleRef("owner-1")

        // change from +1 to -1: delta = -1 - 1 = -2
        service.castVote(userId, CastVoteRequest(articleId, "ARTICLE", -1))

        assertEquals((-1).toShort(), existing.value)
        val event = capturedEvent()
        assertEquals("-2", event.payload["delta"])
    }

    // ── U-20 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-20 castVote retract value 0 - delta is negative of previous value`() {
        val userId    = UUID.randomUUID()
        val articleId = UUID.randomUUID()
        val existing  = Vote().apply {
            id              = UUID.randomUUID()
            this.userId     = userId
            this.targetId   = articleId
            this.targetType = "ARTICLE"
            this.value      = 1
            this.createdAt  = Instant.now()
        }
        every { voteRepo.findByUserAndTarget(userId, articleId, "ARTICLE") } returns existing
        every { outboxRepo.persist(any<OutboxEntry>()) } just Runs
        every { postApiClient.getArticle(articleId) } returns ArticleRef("owner-1")

        service.castVote(userId, CastVoteRequest(articleId, "ARTICLE", 0))

        assertEquals(0.toShort(), existing.value)
        val event = capturedEvent()
        assertEquals("-1", event.payload["delta"])
    }
}

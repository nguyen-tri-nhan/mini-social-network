package com.nhan.social.post.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.post.dto.ArticleQuery
import com.nhan.social.post.dto.CreateArticleRequest
import com.nhan.social.post.entity.Article
import com.nhan.social.post.repository.ArticleRepository
import com.nhan.social.post.repository.OutboxRepository
import io.mockk.*
import io.quarkus.panache.common.Sort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class ArticleServiceTest {

    private val repo           = mockk<ArticleRepository>()
    private val outboxRepo     = mockk<OutboxRepository>()
    private val counterService = mockk<CounterService>()
    private val service        = ArticleService(repo, outboxRepo, counterService, jacksonObjectMapper())

    private fun article(
        authorId: UUID = UUID.randomUUID(),
        visible: Boolean = true,
        voteCount: Int = 5,
        commentCount: Int = 3,
    ) = Article().apply {
        this.id           = UUID.randomUUID()
        this.authorId     = authorId
        this.description  = "Hello world"
        this.visible      = visible
        this.voteCount    = voteCount
        this.commentCount = commentCount
        this.createdAt    = Instant.now()
        this.updatedAt    = Instant.now()
    }

    // ── U-07 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-07 listArticles no filter - returns all visible articles paged`() {
        val items = listOf(article(), article())
        every { repo.findFiltered(any(), any(), any(), 0, 10) } returns items
        every { repo.countFiltered(any(), any()) }              returns 2
        every { counterService.readLiveCounts(any()) }          returns Pair(-1, -1)

        val result = service.listArticles(ArticleQuery(page = 0, size = 10))

        assertEquals(2, result.total)
        assertEquals(2, result.items.size)
        assertFalse(result.hasNext)
    }

    // ── U-08 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-08 listArticles RSQL filter by authorId - repo called with filter params`() {
        val authorId = UUID.randomUUID()
        val items    = listOf(article(authorId = authorId))
        every { repo.findFiltered(any(), any(), any(), 0, 10) } returns items
        every { repo.countFiltered(any(), any()) }              returns 1
        every { counterService.readLiveCounts(any()) }          returns Pair(-1, -1)

        val result = service.listArticles(ArticleQuery(filter = "authorId==\"$authorId\""))

        assertEquals(1, result.total)
        assertEquals(authorId.toString(), result.items[0].authorId)
        verify { repo.findFiltered(any(), any(), any(), 0, 10) }
    }

    // ── U-11 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-11 getById visible article - returns dto`() {
        val art = article()
        every { repo.findById(art.id) } returns art

        val result = service.getById(art.id)

        assertEquals(art.id.toString(), result.id)
        assertEquals("Hello world", result.description)
    }

    // ── U-12 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-12 getById hidden article - throws NotFoundException`() {
        val art = article(visible = false)
        every { repo.findById(art.id) } returns art

        assertThrows<NotFoundException> { service.getById(art.id) }
    }

    @Test
    fun `U-12b getById non-existent - throws NotFoundException`() {
        every { repo.findById(any()) } returns null

        assertThrows<NotFoundException> { service.getById(UUID.randomUUID()) }
    }

    // ── U-13 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-13 delete own article - sets visible to false`() {
        val ownerId = UUID.randomUUID()
        val art     = article(authorId = ownerId)
        every { repo.findById(art.id) } returns art

        service.delete(art.id, requesterId = ownerId)

        assertFalse(art.visible)
    }

    // ── U-14 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-14 delete other's article - throws ForbiddenException`() {
        val art = article(authorId = UUID.randomUUID())
        every { repo.findById(art.id) } returns art

        assertThrows<ForbiddenException> {
            service.delete(art.id, requesterId = UUID.randomUUID())
        }
        assertTrue(art.visible)
    }

    // ── live counts applied when Redis has data ────────────────────────────────

    @Test
    fun `listArticles uses live Redis counts when available`() {
        val art = article(voteCount = 5, commentCount = 3)
        every { repo.findFiltered(any(), any(), any(), 0, 10) } returns listOf(art)
        every { repo.countFiltered(any(), any()) }              returns 1
        every { counterService.readLiveCounts(art.id.toString()) } returns Pair(10, 7)

        val result = service.listArticles(ArticleQuery())

        assertEquals(10, result.items[0].voteCount)
        assertEquals(7, result.items[0].commentCount)
    }

    @Test
    fun `listArticles falls back to DB counts when Redis miss`() {
        val art = article(voteCount = 5, commentCount = 3)
        every { repo.findFiltered(any(), any(), any(), 0, 10) } returns listOf(art)
        every { repo.countFiltered(any(), any()) }              returns 1
        every { counterService.readLiveCounts(any()) }          returns Pair(-1, -1)

        val result = service.listArticles(ArticleQuery())

        assertEquals(5, result.items[0].voteCount)
        assertEquals(3, result.items[0].commentCount)
    }
}

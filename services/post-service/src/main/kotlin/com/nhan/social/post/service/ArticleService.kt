package com.nhan.social.post.service

import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.articleCreatedEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.post.dto.ArticleDto
import com.nhan.social.post.dto.CreateArticleRequest
import com.nhan.social.post.dto.toDto
import com.nhan.social.post.entity.Article
import com.nhan.social.post.repository.ArticleRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.MutinyEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Channel
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class ArticleService(
    private val repo: ArticleRepository,
    private val counterService: CounterService,
    private val objectMapper: ObjectMapper,
) {
    @Inject
    @Channel("social-events-out")
    lateinit var emitter: MutinyEmitter<String>

    fun listArticles(page: Int, size: Int): PageResponse<ArticleDto> {
        val items = repo.findVisible(page, size).map { article ->
            val (liveVotes, liveComments) = counterService.readLiveCounts(article.id.toString())
            article.toDto().copy(
                voteCount    = if (liveVotes    >= 0) liveVotes    else article.voteCount,
                commentCount = if (liveComments >= 0) liveComments else article.commentCount,
            )
        }
        val total = repo.countVisible()
        return PageResponse(items = items, total = total, page = page, size = size, hasNext = (page + 1) * size < total)
    }

    fun getById(id: UUID): ArticleDto =
        repo.findById(id)?.takeIf { it.visible }?.toDto()
            ?: throw NotFoundException("Article $id not found")

    @Transactional
    fun create(authorId: UUID, request: CreateArticleRequest): ArticleDto {
        val article = Article().apply {
            this.id = UUID.randomUUID()
            this.authorId = authorId
            this.description = request.description
            this.imageUrl = request.imageUrl
            this.visible = true
            this.createdAt = Instant.now()
            this.updatedAt = Instant.now()
        }
        repo.persist(article)

        val event = articleCreatedEvent(article.id.toString(), authorId.toString())
        emitter.sendAndAwait(objectMapper.writeValueAsString(event))
        return article.toDto()
    }

    @Transactional
    fun delete(id: UUID, requesterId: UUID): ArticleDto {
        val article = repo.findById(id) ?: throw NotFoundException("Article $id not found")
        if (article.authorId != requesterId) throw ForbiddenException()
        article.visible = false
        article.updatedAt = Instant.now()
        return article.toDto()
    }
}

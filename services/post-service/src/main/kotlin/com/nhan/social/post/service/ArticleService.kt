package com.nhan.social.post.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.common.rsql.RsqlQuerySpec
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.post.dto.ArticleDto
import com.nhan.social.post.dto.ArticleQuery
import com.nhan.social.post.dto.CreateArticleRequest
import com.nhan.social.post.dto.toDto
import com.nhan.social.post.entity.Article
import com.nhan.social.post.entity.OutboxEntry
import com.nhan.social.post.repository.ArticleRepository
import com.nhan.social.post.repository.OutboxRepository
import com.nhan.social.post.rsql.parseArticleFilter
import com.nhan.social.post.rsql.parseArticleSort
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class ArticleService(
    private val repo: ArticleRepository,
    private val outboxRepo: OutboxRepository,
    private val counterService: CounterService,
    private val objectMapper: ObjectMapper,
) {
    fun listArticles(query: ArticleQuery): PageResponse<ArticleDto> {
        val spec = if (query.filter.isNullOrBlank()) RsqlQuerySpec.of("visible = true") else parseArticleFilter(query.filter)
        return fetchArticles(query.page, query.size.coerceAtMost(50), spec, parseArticleSort(query.sort))
    }

    fun getById(id: UUID): ArticleDto =
        repo.findById(id)?.takeIf { it.visible }?.toDto()
            ?: throw NotFoundException("Article $id not found")

    @Transactional
    fun create(authorId: UUID, request: CreateArticleRequest): ArticleDto {
        val article = Article().apply {
            this.id          = UUID.randomUUID()
            this.authorId    = authorId
            this.description = request.description
            this.imageUrl    = request.imageUrl
            this.visible     = true
            this.createdAt   = Instant.now()
            this.updatedAt   = Instant.now()
        }
        repo.persist(article)

        outboxRepo.persist(outbox(
            aggregateType = "post",
            aggregateId   = article.id,
            event = SocialEvent(
                eventType = EventType.ARTICLE_CREATED,
                payload = mapOf(
                    "articleId" to article.id.toString(),
                    "authorId"  to authorId.toString(),
                ),
            ),
        ))

        return article.toDto()
    }

    @Transactional
    fun delete(id: UUID, requesterId: UUID): ArticleDto {
        val article = repo.findById(id) ?: throw NotFoundException("Article $id not found")
        if (article.authorId != requesterId) throw ForbiddenException()
        article.visible   = false
        article.updatedAt = Instant.now()
        return article.toDto()
    }

    private fun outbox(aggregateType: String, aggregateId: UUID, event: SocialEvent): OutboxEntry =
        OutboxEntry().apply {
            this.aggregateType = aggregateType
            this.aggregateId   = aggregateId
            this.eventType     = event.eventType.name
            this.payload       = objectMapper.writeValueAsString(event.copy(eventId = this.id.toString()))
        }

    private fun fetchArticles(page: Int, size: Int, spec: RsqlQuerySpec, sort: Sort): PageResponse<ArticleDto> {
        val items = repo.findFiltered(spec.hql, spec.params, sort, page, size).map { article ->
            val (liveVotes, liveComments) = counterService.readLiveCounts(article.id.toString())
            article.toDto().copy(
                voteCount    = if (liveVotes    >= 0) liveVotes    else article.voteCount,
                commentCount = if (liveComments >= 0) liveComments else article.commentCount,
            )
        }
        val total = repo.countFiltered(spec.hql, spec.params)
        return PageResponse(items = items, total = total, page = page, size = size, hasNext = (page + 1) * size < total)
    }
}

package com.nhan.social.interaction.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.interaction.client.PostApiClient
import com.nhan.social.interaction.dto.CastVoteRequest
import com.nhan.social.interaction.dto.CommentDto
import com.nhan.social.interaction.dto.CreateCommentRequest
import com.nhan.social.interaction.dto.VoteDto
import com.nhan.social.interaction.dto.toDto
import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.OutboxEntry
import com.nhan.social.interaction.entity.UserRef
import com.nhan.social.interaction.entity.Vote
import com.nhan.social.interaction.repository.CommentRepository
import com.nhan.social.interaction.repository.OutboxRepository
import com.nhan.social.interaction.repository.UserRefRepository
import com.nhan.social.interaction.repository.VoteRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class InteractionService(
    private val commentRepo: CommentRepository,
    private val voteRepo: VoteRepository,
    private val outboxRepo: OutboxRepository,
    private val userRefRepo: UserRefRepository,
    private val objectMapper: ObjectMapper,
    @RestClient private val postApiClient: PostApiClient,
) {
    private val log = Logger.getLogger(InteractionService::class.java)

    fun listComments(targetId: UUID, targetType: String, page: Int, size: Int): PageResponse<CommentDto> {
        val comments = commentRepo.findByTarget(targetId, targetType, page, size)
        val authors = userRefRepo.findByIds(comments.map { it.authorId }.distinct()).associateBy { it.id }
        val items = comments.map { it.toDto(authors[it.authorId]) }
        val total = commentRepo.countByTarget(targetId, targetType)
        return PageResponse(items = items, total = total, page = page, size = size, hasNext = (page + 1) * size < total)
    }

    @Transactional
    fun addComment(authorId: UUID, request: CreateCommentRequest): CommentDto {
        val comment = Comment().apply {
            this.id          = UUID.randomUUID()
            this.targetId    = request.targetId
            this.targetType  = request.targetType.uppercase()
            this.authorId    = authorId
            this.description = request.description
            this.createdAt   = Instant.now()
            this.updatedAt   = Instant.now()
        }
        commentRepo.persist(comment)

        val (ownerIdStr, articleIdStr) = resolveOwner(request.targetId, comment.targetType)
        val actor = userRefRepo.findById(authorId)

        outboxRepo.persist(outbox(
            aggregateType = "interaction",
            aggregateId   = comment.id,
            event = SocialEvent(
                eventType = EventType.COMMENT_CREATED,
                payload = buildMap {
                    put("commentId",  comment.id.toString())
                    put("articleId",  articleIdStr ?: request.targetId.toString())
                    put("targetType", comment.targetType)
                    put("actorId",    authorId.toString())
                    if (ownerIdStr != null) put("articleAuthorId", ownerIdStr)
                    putActor(actor)
                },
            ),
        ))

        return comment.toDto(actor)
    }

    @Transactional
    fun deleteComment(commentId: UUID, requesterId: UUID) {
        val comment = commentRepo.findById(commentId) ?: throw NotFoundException("Comment $commentId not found")
        if (comment.authorId != requesterId) throw ForbiddenException()
        comment.visible   = false
        comment.updatedAt = Instant.now()
    }

    @Transactional
    fun castVote(userId: UUID, request: CastVoteRequest): VoteDto {
        val targetId   = request.targetId
        val targetType = request.targetType.uppercase()
        val newValue   = request.value.toShort()

        val existing = voteRepo.findByUserAndTarget(userId, targetId, targetType)
        val oldValue = existing?.value?.toLong() ?: 0L
        val delta    = newValue - oldValue

        val vote = if (existing != null) {
            existing.value = newValue
            existing
        } else {
            Vote().apply {
                this.id         = UUID.randomUUID()
                this.userId     = userId
                this.targetId   = targetId
                this.targetType = targetType
                this.value      = newValue
                this.createdAt  = Instant.now()
            }.also { voteRepo.persist(it) }
        }

        val (ownerIdStr, articleIdStr) = resolveOwner(targetId, targetType)
        val actor = userRefRepo.findById(userId)

        outboxRepo.persist(outbox(
            aggregateType = "interaction",
            aggregateId   = vote.id,
            event = SocialEvent(
                eventType = EventType.VOTE_CAST,
                payload = buildMap {
                    put("targetId",   targetId.toString())
                    put("targetType", targetType)
                    put("actorId",    userId.toString())
                    put("delta",      delta.toString())
                    if (articleIdStr != null) put("articleId",      articleIdStr)
                    if (ownerIdStr   != null) put("targetAuthorId", ownerIdStr)
                    putActor(actor)
                },
            ),
        ))

        return vote.toDto()
    }

    // Returns (ownerId, articleId) by target type.
    // ARTICLE → fetch authorId from post-api; articleId = targetId.
    // COMMENT → look up locally; articleId = comment.targetId if that comment targets an ARTICLE.
    private fun resolveOwner(targetId: UUID, targetType: String): Pair<String?, String?> =
        when (targetType) {
            "ARTICLE" -> {
                val authorId = try {
                    postApiClient.getArticle(targetId).authorId
                } catch (e: Exception) {
                    log.warnf("Could not fetch article author for %s: %s", targetId, e.message)
                    null
                }
                Pair(authorId, targetId.toString())
            }
            "COMMENT" -> {
                val comment = commentRepo.findById(targetId)
                val authorId = comment?.authorId?.toString()
                val articleId = comment?.takeIf { it.targetType == "ARTICLE" }?.targetId?.toString()
                Pair(authorId, articleId)
            }
            else -> Pair(null, null)
        }

    private fun outbox(aggregateType: String, aggregateId: UUID, event: SocialEvent): OutboxEntry =
        OutboxEntry().apply {
            this.aggregateType = aggregateType
            this.aggregateId   = aggregateId
            this.eventType     = event.eventType.name
            this.payload       = objectMapper.writeValueAsString(event.copy(eventId = this.id.toString()))
        }

    // Nhúng tên actor (từ user_ref cục bộ) vào event — để notification-service
    // và websocket-service đọc trực tiếp, không cần tự giữ cache riêng.
    private fun MutableMap<String, String>.putActor(actor: UserRef?) {
        if (actor == null) return
        put("actorUsername", actor.username)
        put("actorFirstname", actor.firstname)
        put("actorLastname", actor.lastname)
        actor.avatarUrl?.let { put("actorAvatarUrl", it) }
    }
}

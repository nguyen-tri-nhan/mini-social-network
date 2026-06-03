package com.nhan.social.interaction.service

import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.commentCreatedEvent
import com.nhan.social.common.event.voteCastEvent
import com.nhan.social.exception.ErrorType
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.exception.ServiceCode
import com.nhan.social.exception.errorCode
import com.nhan.social.interaction.dto.*
import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.Vote
import com.nhan.social.interaction.repository.CommentRepository
import com.nhan.social.interaction.repository.VoteRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.MutinyEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Channel
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class InteractionService(
    private val commentRepo: CommentRepository,
    private val voteRepo: VoteRepository,
    private val objectMapper: ObjectMapper,
) {
    @Inject
    @Channel("social-events-out")
    lateinit var emitter: MutinyEmitter<String>

    fun listComments(articleId: UUID, page: Int, size: Int): PageResponse<CommentDto> {
        val items = commentRepo.findByArticle(articleId, page, size).map { it.toDto() }
        val total = commentRepo.countByArticle(articleId)
        return PageResponse(
            items = items, total = total, page = page, size = size,
            hasNext = (page + 1) * size < total,
        )
    }

    @Transactional
    fun addComment(articleId: UUID, authorId: UUID, request: CreateCommentRequest): CommentDto {
        val comment = Comment().apply {
            this.id = UUID.randomUUID()
            this.articleId = articleId
            this.authorId = authorId
            this.description = request.description
            this.createdAt = Instant.now()
            this.updatedAt = Instant.now()
        }
        commentRepo.persist(comment)

        // Fire-and-forget: publish event, do NOT wait for counter update
        val event = commentCreatedEvent(
            commentId = comment.id.toString(),
            articleId = articleId.toString(),
            actorId = authorId.toString(),
            articleAuthorId = "",  // enriched by notification-service via post lookup
        )
        emitter.sendAndAwait(objectMapper.writeValueAsString(event))
        return comment.toDto()
    }

    @Transactional
    fun deleteComment(commentId: UUID, requesterId: UUID) {
        val comment = commentRepo.findById(commentId) ?: throw NotFoundException("Comment not found", errorCode(ServiceCode.INTERACTION, ErrorType.NOT_FOUND))
        if (comment.authorId != requesterId) throw ForbiddenException(errorCode = errorCode(ServiceCode.INTERACTION, ErrorType.FORBIDDEN))
        comment.visible = false
        comment.updatedAt = Instant.now()
    }

    @Transactional
    fun castVote(targetId: UUID, targetType: String, userId: UUID, request: CastVoteRequest): VoteDto {
        val existing = voteRepo.findByUserAndTarget(userId, targetId, targetType)
        val oldValue = existing?.value?.toLong() ?: 0L
        val newValue = request.value.toShort()

        val vote = if (existing != null) {
            existing.value = newValue
            existing
        } else {
            Vote().apply {
                this.id = UUID.randomUUID()
                this.userId = userId
                this.targetId = targetId
                this.targetType = targetType
                this.value = newValue
                this.createdAt = Instant.now()
            }.also { voteRepo.persist(it) }
        }

        val delta = newValue - oldValue
        val event = voteCastEvent(
            targetId = targetId.toString(),
            targetType = targetType,
            actorId = userId.toString(),
            targetAuthorId = "",
        ).copy(payload = mapOf(
            "targetId" to targetId.toString(),
            "targetType" to targetType,
            "actorId" to userId.toString(),
            "targetAuthorId" to "",
            "delta" to delta.toString(),
        ))
        emitter.sendAndAwait(objectMapper.writeValueAsString(event))
        return vote.toDto()
    }
}

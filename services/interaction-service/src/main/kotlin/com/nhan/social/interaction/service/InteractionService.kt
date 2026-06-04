package com.nhan.social.interaction.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.commentCreatedEvent
import com.nhan.social.common.event.voteCastEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.interaction.dto.CastVoteRequest
import com.nhan.social.interaction.dto.CommentDto
import com.nhan.social.interaction.dto.CreateCommentRequest
import com.nhan.social.interaction.dto.VoteDto
import com.nhan.social.interaction.dto.toDto
import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.Vote
import com.nhan.social.interaction.repository.CommentRepository
import com.nhan.social.interaction.repository.VoteRepository
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

    fun listComments(targetId: UUID, targetType: String, page: Int, size: Int): PageResponse<CommentDto> {
        val items = commentRepo.findByTarget(targetId, targetType, page, size).map { it.toDto() }
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

        val event = commentCreatedEvent(
            commentId       = comment.id.toString(),
            articleId       = request.targetId.toString(),
            actorId         = authorId.toString(),
            articleAuthorId = "",
        )
        emitter.sendAndAwait(objectMapper.writeValueAsString(event))
        return comment.toDto()
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

        val delta = newValue - oldValue
        val event = voteCastEvent(
            targetId       = targetId.toString(),
            targetType     = targetType,
            actorId        = userId.toString(),
            targetAuthorId = "",
        ).copy(payload = mapOf(
            "targetId"       to targetId.toString(),
            "targetType"     to targetType,
            "actorId"        to userId.toString(),
            "targetAuthorId" to "",
            "delta"          to delta.toString(),
        ))
        emitter.sendAndAwait(objectMapper.writeValueAsString(event))
        return vote.toDto()
    }
}

package com.nhan.social.interaction.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.dto.PageResponse
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ForbiddenException
import com.nhan.social.exception.NotFoundException
import com.nhan.social.interaction.dto.CastVoteRequest
import com.nhan.social.interaction.dto.CommentDto
import com.nhan.social.interaction.dto.CreateCommentRequest
import com.nhan.social.interaction.dto.VoteDto
import com.nhan.social.interaction.dto.toDto
import com.nhan.social.interaction.entity.Comment
import com.nhan.social.interaction.entity.OutboxEntry
import com.nhan.social.interaction.entity.Vote
import com.nhan.social.interaction.repository.CommentRepository
import com.nhan.social.interaction.repository.OutboxRepository
import com.nhan.social.interaction.repository.VoteRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class InteractionService(
    private val commentRepo: CommentRepository,
    private val voteRepo: VoteRepository,
    private val outboxRepo: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
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

        outboxRepo.persist(outbox(
            aggregateType = "comment",
            aggregateId   = comment.id,
            event = SocialEvent(
                eventType = EventType.COMMENT_CREATED,
                payload = mapOf(
                    "commentId"  to comment.id.toString(),
                    "targetId"   to comment.targetId.toString(),
                    "targetType" to comment.targetType,
                    "actorId"    to authorId.toString(),
                ),
            ),
        ))

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

        outboxRepo.persist(outbox(
            aggregateType = "vote",
            aggregateId   = vote.id,
            event = SocialEvent(
                eventType = EventType.VOTE_CAST,
                payload = mapOf(
                    "targetId"   to targetId.toString(),
                    "targetType" to targetType,
                    "actorId"    to userId.toString(),
                    "delta"      to delta.toString(),
                ),
            ),
        ))

        return vote.toDto()
    }

    private fun outbox(aggregateType: String, aggregateId: UUID, event: SocialEvent): OutboxEntry =
        OutboxEntry().apply {
            this.aggregateType = aggregateType
            this.aggregateId   = aggregateId
            this.eventType     = event.eventType.name
            this.payload       = objectMapper.writeValueAsString(event.copy(eventId = this.id.toString()))
        }
}

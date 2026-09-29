package com.nhan.social.post.consumer

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.post.service.CounterService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InteractionEventConsumerTest {

    private val objectMapper   = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val counterService = mockk<CounterService> {
        every { incrementComment(any(), any()) } just Runs
        every { incrementVote(any(), any(), any()) } just Runs
    }
    private val consumer = InteractionEventConsumer(counterService, objectMapper)

    private fun send(type: EventType, payload: Map<String, String>) =
        consumer.consume(objectMapper.writeValueAsString(SocialEvent(type, payload = payload, eventId = "evt-1")))

    // Payload thật từ InteractionService.addComment() — không có key "targetId".
    @Test
    fun `comment created increments comment count of articleId`() {
        send(EventType.COMMENT_CREATED, mapOf(
            "commentId" to "c-1", "articleId" to "a-1", "targetType" to "ARTICLE", "actorId" to "u-1",
        ))

        verify { counterService.incrementComment("a-1", "evt-1") }
    }

    @Test
    fun `vote on article increments vote count by delta`() {
        send(EventType.VOTE_CAST, mapOf("targetId" to "a-1", "targetType" to "ARTICLE", "delta" to "-2"))

        verify { counterService.incrementVote("a-1", -2L, "evt-1") }
    }

    @Test
    fun `vote on comment does not touch article counters`() {
        send(EventType.VOTE_CAST, mapOf("targetId" to "c-1", "targetType" to "COMMENT", "delta" to "1"))

        verify(exactly = 0) { counterService.incrementVote(any(), any(), any()) }
    }

    @Test
    fun `malformed message propagates so it goes to the dead-letter topic`() {
        assertThrows<Exception> { consumer.consume("not json") }
    }
}

package com.nhan.social.post.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.post.service.CounterService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

// Không catch exception: lỗi → message sang dead-letter topic
// (failure-strategy trong application.properties) thay vì mất âm thầm.
@ApplicationScoped
class InteractionEventConsumer(
    private val counterService: CounterService,
    private val objectMapper: ObjectMapper,
) {
    @Incoming("interaction-events-in")
    fun consume(message: String) {
        val event   = objectMapper.readValue(message, SocialEvent::class.java)
        val eventId = event.eventId

        when (event.eventType) {
            EventType.COMMENT_CREATED -> {
                val articleId = event.payload["articleId"] ?: return
                counterService.incrementComment(articleId, eventId)
            }
            EventType.VOTE_CAST -> {
                val targetId = event.payload["targetId"] ?: return
                val delta    = event.payload["delta"]?.toLongOrNull() ?: 1L
                if (event.payload["targetType"] == "ARTICLE") {
                    counterService.incrementVote(targetId, delta, eventId)
                }
            }
            else -> { /* not handled here */ }
        }
    }
}

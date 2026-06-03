package com.nhan.social.post.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.post.service.CounterService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class InteractionEventConsumer(
    private val counterService: CounterService,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(InteractionEventConsumer::class.java)

    @Incoming("interaction-events-in")
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, SocialEvent::class.java)
            when (event.eventType) {
                EventType.COMMENT_CREATED -> {
                    val articleId = event.payload["articleId"] ?: return
                    counterService.incrementComment(articleId)
                }
                EventType.VOTE_CAST -> {
                    val targetId = event.payload["targetId"] ?: return
                    if (event.payload["targetType"] == "ARTICLE") {
                        counterService.incrementVote(targetId, event.payload["delta"]?.toLongOrNull() ?: 1L)
                    }
                }
                else -> { /* not handled here */ }
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to process interaction event: %s", message)
        }
    }
}

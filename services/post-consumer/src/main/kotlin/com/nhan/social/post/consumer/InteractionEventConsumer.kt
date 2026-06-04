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
            val event   = objectMapper.readValue(message, SocialEvent::class.java)
            val eventId = event.eventId

            when (event.eventType) {
                EventType.COMMENT_CREATED -> {
                    val articleId = event.payload["targetId"] ?: return
                    counterService.incrementComment(articleId, eventId)
                }
                EventType.VOTE_CAST -> {
                    val targetId = event.payload["targetId"]   ?: return
                    val delta    = event.payload["delta"]?.toLongOrNull() ?: 1L
                    if (event.payload["targetType"] == "ARTICLE") {
                        counterService.incrementVote(targetId, delta, eventId)
                    }
                }
                else -> { /* not handled here */ }
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to process interaction event: %s", message)
        }
    }
}

package com.nhan.social.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class WsEventConsumer(
    private val wsPushService: WsPushService,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(WsEventConsumer::class.java)

    @Incoming("interaction-events-in")
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, SocialEvent::class.java)
            when (event.eventType) {
                EventType.COMMENT_CREATED -> handleComment(event)
                EventType.VOTE_CAST       -> handleVote(event)
                else                      -> { }
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to process ws event: %s", message)
        }
    }

    private fun handleComment(event: SocialEvent) {
        val payload = event.payload
        val articleId    = payload["articleId"]       ?: return
        val authorId     = payload["articleAuthorId"]

        // Push live comment to anyone viewing this article
        wsPushService.push(
            topic   = "article_${articleId}_comment_added",
            message = ServerMessage(
                topic   = "article_${articleId}_comment_added",
                type    = "COMMENT_ADDED",
                payload = payload,
            ),
        )

        // Push notification to article author
        if (authorId != null) {
            wsPushService.push(
                topic   = "user_${authorId}_notification",
                message = ServerMessage(
                    topic   = "user_${authorId}_notification",
                    type    = "NOTIFICATION",
                    payload = payload,
                ),
            )
        }
    }

    private fun handleVote(event: SocialEvent) {
        val payload      = event.payload
        val targetAuthor = payload["targetAuthorId"] ?: return

        wsPushService.push(
            topic   = "user_${targetAuthor}_notification",
            message = ServerMessage(
                topic   = "user_${targetAuthor}_notification",
                type    = "NOTIFICATION",
                payload = payload,
            ),
        )
    }
}

package com.nhan.social.notification.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.notification.service.NotificationService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class NotificationEventConsumer(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(NotificationEventConsumer::class.java)

    @Incoming("social-events-in")
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, SocialEvent::class.java)
            when (event.eventType) {
                EventType.COMMENT_CREATED, EventType.VOTE_CAST ->
                    notificationService.createFromEvent(event)
                else -> { /* not handled */ }
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to process notification event: %s", message)
        }
    }
}

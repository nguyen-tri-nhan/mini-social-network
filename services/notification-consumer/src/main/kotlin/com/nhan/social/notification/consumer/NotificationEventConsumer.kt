package com.nhan.social.notification.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.notification.service.NotificationService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

// Không catch exception: lỗi → message sang dead-letter topic
// (failure-strategy trong application.properties) thay vì mất âm thầm.
@ApplicationScoped
class NotificationEventConsumer(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper,
) {
    @Incoming("social-events-in")
    fun consume(message: String) {
        val event = objectMapper.readValue(message, SocialEvent::class.java)
        when (event.eventType) {
            EventType.COMMENT_CREATED, EventType.VOTE_CAST ->
                notificationService.createFromEvent(event)
            else -> { /* not handled */ }
        }
    }
}

package com.nhan.social.user.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.user.service.UserService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class UserEventConsumer(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(UserEventConsumer::class.java)

    @Incoming("social-events-in")
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, SocialEvent::class.java)
            when (event.eventType) {
                EventType.USER_UPDATED -> {
                    val action = event.payload["action"]
                    if (action == "CREATED") userService.createFromEvent(event)
                }
                else -> { /* not handled here */ }
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to process event: %s", message)
        }
    }
}

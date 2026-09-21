package com.nhan.social.interaction.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.interaction.entity.UserRef
import com.nhan.social.interaction.repository.UserRefRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

// Giữ materialized view userId→tên (topic social.user) để enrich Comment/Vote
// mà không phải gọi mạng sang user-service. Xem specs/decisions/0006.
@ApplicationScoped
class UserRefEventConsumer(
    private val repo: UserRefRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(UserRefEventConsumer::class.java)

    @Incoming("user-events-in")
    @Transactional
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, SocialEvent::class.java)
            if (event.eventType != EventType.USER_READY && event.eventType != EventType.USER_PROFILE_UPDATED) return

            val payload = event.payload
            val userId = UUID.fromString(payload["userId"] ?: return)

            val ref = repo.findById(userId) ?: UserRef().apply { id = userId }
            ref.username  = payload["username"] ?: ""
            ref.firstname = payload["firstname"] ?: ""
            ref.lastname  = payload["lastname"] ?: ""
            ref.avatarUrl = payload["avatarUrl"]?.takeIf { it.isNotBlank() }
            ref.updatedAt = Instant.now()
            repo.persist(ref)
        } catch (e: Exception) {
            log.errorf(e, "Failed to process user ref event: %s", message)
        }
    }
}

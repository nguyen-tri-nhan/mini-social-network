package com.nhan.social.chat.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.chat.entity.UserRef
import com.nhan.social.chat.repository.UserRefRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Incoming
import java.time.Instant
import java.util.UUID

// Tên/avatar người kia trong danh sách conversation — cùng pattern interaction-service (ADR 0006).
// Không catch exception: lỗi → dead-letter topic (xem application.properties).
@ApplicationScoped
class UserRefEventConsumer(
    private val repo: UserRefRepository,
    private val objectMapper: ObjectMapper,
) {
    @Incoming("user-events-in")
    @Transactional
    fun consume(message: String) {
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
    }
}

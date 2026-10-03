package com.nhan.social.chat.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "conversation_participant")
class ConversationParticipant : PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    lateinit var conversationId: UUID

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    lateinit var userId: UUID

    // UUIDv7 của tin cuối đã đọc — so sánh id thay cho timestamp (§13.3).
    @Column(name = "last_read_message_id", columnDefinition = "uuid")
    var lastReadMessageId: UUID? = null

    @Column(name = "joined_at", updatable = false)
    var joinedAt: Instant = Instant.now()
}

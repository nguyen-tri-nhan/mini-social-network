package com.nhan.social.chat.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// Chỉ có bản mã — không có cột nội dung dạng thường (ADR 0007).
@Entity
@Table(name = "chat_message")
class ChatMessage : PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    lateinit var id: UUID

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    lateinit var conversationId: UUID

    @Column(name = "sender_id", nullable = false, columnDefinition = "uuid")
    lateinit var senderId: UUID

    @Column(name = "client_message_id", nullable = false, columnDefinition = "uuid")
    lateinit var clientMessageId: UUID

    @Column(name = "key_version", nullable = false)
    var keyVersion: Int = 1

    @Column(nullable = false, columnDefinition = "bytea")
    lateinit var nonce: ByteArray

    @Column(nullable = false, columnDefinition = "bytea")
    lateinit var ciphertext: ByteArray

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

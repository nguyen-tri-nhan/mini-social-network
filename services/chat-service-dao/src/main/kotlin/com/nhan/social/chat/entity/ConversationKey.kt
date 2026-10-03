package com.nhan.social.chat.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class ConversationKeyId(var conversationId: UUID? = null, var version: Int = 0) : Serializable

@Entity
@Table(name = "conversation_key")
@IdClass(ConversationKeyId::class)
class ConversationKey : PanacheEntityBase {
    @Id
    @Column(name = "conversation_id", columnDefinition = "uuid")
    lateinit var conversationId: UUID

    @Id
    @Column(name = "version")
    var version: Int = 1

    // CiphertextBlob từ KMS — vô dụng nếu không gọi được KMS Decrypt (ADR 0007).
    @Column(name = "encrypted_data_key", nullable = false, columnDefinition = "bytea")
    lateinit var encryptedDataKey: ByteArray

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

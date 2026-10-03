package com.nhan.social.chat.dto

import com.nhan.social.chat.entity.UserRef
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateConversationRequest(
    @field:NotNull val targetUserId: UUID? = null,
)

// 4.096 vì giới hạn ~1 MB của Kafka trên đường outbox, không phải vì cột lưu (§13.2).
data class SendMessageRequest(
    @field:NotNull val clientMessageId: UUID? = null,
    @field:NotBlank @field:Size(max = 4096) val content: String = "",
)

data class MarkReadRequest(
    @field:NotNull val messageId: UUID? = null,
)

data class ChatUserDto(
    val id: String,
    val username: String?,
    val firstname: String?,
    val lastname: String?,
    val avatarUrl: String?,
)

data class MessageDto(
    val id: String,
    val clientMessageId: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
)

data class ConversationDto(
    val id: String,
    val otherUser: ChatUserDto,
    val lastMessage: MessageDto?,
    val unread: Boolean,
)

data class CursorPage<T>(
    val items: List<T>,
    val hasMore: Boolean,
)

data class UnreadCountDto(val count: Long)

fun chatUser(id: UUID, ref: UserRef?) = ChatUserDto(
    id        = id.toString(),
    username  = ref?.username,
    firstname = ref?.firstname,
    lastname  = ref?.lastname,
    avatarUrl = ref?.avatarUrl,
)

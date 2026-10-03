package com.nhan.social.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.chat.crypto.ChatCipher
import com.nhan.social.chat.crypto.DataKeyProvider
import com.nhan.social.chat.dto.ConversationDto
import com.nhan.social.chat.dto.CursorPage
import com.nhan.social.chat.dto.MessageDto
import com.nhan.social.chat.dto.chatUser
import com.nhan.social.chat.entity.ChatMessage
import com.nhan.social.chat.entity.ConversationKey
import com.nhan.social.chat.entity.ConversationParticipant
import com.nhan.social.chat.entity.OutboxEntry
import com.nhan.social.chat.repository.ChatMessageRepository
import com.nhan.social.chat.repository.ConversationKeyRepository
import com.nhan.social.chat.repository.ConversationRepository
import com.nhan.social.chat.repository.OutboxRepository
import com.nhan.social.chat.repository.ParticipantRepository
import com.nhan.social.chat.repository.UserRefRepository
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.common.id.Uuid7
import com.nhan.social.exception.BadRequestException
import com.nhan.social.exception.ConflictException
import com.nhan.social.exception.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.Base64
import java.util.UUID

@ApplicationScoped
class ChatService(
    private val conversationRepo: ConversationRepository,
    private val participantRepo: ParticipantRepository,
    private val keyRepo: ConversationKeyRepository,
    private val messageRepo: ChatMessageRepository,
    private val userRefRepo: UserRefRepository,
    private val outboxRepo: OutboxRepository,
    private val dataKeys: DataKeyProvider,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun openDirect(me: UUID, targetUserId: UUID): ConversationDto {
        if (me == targetUserId) throw BadRequestException("Cannot start a conversation with yourself")
        userRefRepo.findById(targetUserId) ?: throw NotFoundException("User $targetUserId not found")

        val id = directConversationId(me, targetUserId)
        // Chỉ bên thắng insert mới tạo participant + DEK → không có DEK thừa khi 2 bên bấm cùng lúc.
        if (conversationRepo.insertIfAbsent(id)) {
            participantRepo.persist(participant(id, me), participant(id, targetUserId))
            val key = dataKeys.generate(id, KEY_VERSION)
            keyRepo.persist(ConversationKey().apply {
                conversationId   = id
                version          = KEY_VERSION
                encryptedDataKey = key.wrapped
            })
        }
        return conversation(me, id)
    }

    fun get(me: UUID, conversationId: UUID): ConversationDto {
        requireParticipant(conversationId, me)
        return conversation(me, conversationId)
    }

    fun list(me: UUID, before: UUID?, size: Int): CursorPage<ConversationDto> {
        val fetched = messageRepo.latestPerConversation(me, before, size + 1)
        val latest = fetched.take(size)
        val ids = latest.map { it.conversationId }

        val others = participantRepo.findByConversations(ids)
            .filter { it.userId != me }
            .associate { it.conversationId to it.userId }
        val refs = userRefRepo.findByIds(others.values.toSet()).associateBy { it.id }
        val unread = messageRepo.unreadConversationIds(me, ids)
        val keys = keyRepo.findByConversations(ids).associateBy { it.conversationId to it.version }

        val items = latest.map { last ->
            val otherId = others.getValue(last.conversationId)
            ConversationDto(
                id          = last.conversationId.toString(),
                otherUser   = chatUser(otherId, refs[otherId]),
                lastMessage = last.toDto(decrypt(last, keys[last.conversationId to last.keyVersion]).preview()),
                unread      = last.conversationId in unread,
            )
        }
        return CursorPage(items, hasMore = fetched.size > size)
    }

    fun unreadCount(me: UUID): Long = messageRepo.countUnreadConversations(me)

    @Transactional
    fun markRead(me: UUID, conversationId: UUID, messageId: UUID) {
        requireParticipant(conversationId, me)
        val message = messageRepo.findById(messageId)
        if (message == null || message.conversationId != conversationId) {
            throw NotFoundException("Message $messageId not found in conversation")
        }
        participantRepo.advanceLastRead(conversationId, me, messageId)
    }

    fun messages(me: UUID, conversationId: UUID, before: UUID?, size: Int): CursorPage<MessageDto> {
        requireParticipant(conversationId, me)
        val fetched = messageRepo.page(conversationId, before, size + 1)
        val page = fetched.take(size)
        val keys = keyRepo.findByConversations(listOf(conversationId)).associateBy { it.version }
        return CursorPage(
            items   = page.map { it.toDto(decrypt(it, keys[it.keyVersion])) },
            hasMore = fetched.size > size,
        )
    }

    @Transactional
    fun send(me: UUID, conversationId: UUID, clientMessageId: UUID, content: String): MessageDto {
        val participants = participantRepo.findByConversation(conversationId)
        if (participants.none { it.userId == me }) throw NotFoundException("Conversation $conversationId not found")

        val key = keyRepo.findKey(conversationId, KEY_VERSION)
            ?: throw IllegalStateException("Missing data key for conversation $conversationId")
        val messageId = Uuid7.next()
        val encrypted = ChatCipher.encrypt(
            dataKeys.unwrap(conversationId, key.version, key.encryptedDataKey),
            content,
            ChatCipher.aad(conversationId, messageId, key.version),
        )
        val message = ChatMessage().apply {
            id                   = messageId
            this.conversationId  = conversationId
            senderId             = me
            this.clientMessageId = clientMessageId
            keyVersion           = key.version
            nonce                = encrypted.nonce
            ciphertext           = encrypted.ciphertext
            createdAt            = Instant.ofEpochMilli(Uuid7.timestampMillis(messageId))
        }

        // Gửi lại cùng clientMessageId (retry/mạng chập chờn) → trả tin cũ, không phát event mới (§14.3).
        if (!messageRepo.insertIfAbsent(message)) {
            val existing = messageRepo.findBySenderAndClientId(me, clientMessageId)
                ?: throw ConflictException("Duplicate clientMessageId")
            if (existing.conversationId != conversationId) throw ConflictException("clientMessageId already used")
            return existing.toDto(decrypt(existing, keyRepo.findKey(conversationId, existing.keyVersion)))
        }

        outboxRepo.emit(chatMessageEvent(message, key.encryptedDataKey, participants.map { it.userId }))
        return message.toDto(content)
    }

    private fun conversation(me: UUID, conversationId: UUID): ConversationDto {
        val otherId = participantRepo.findByConversation(conversationId).first { it.userId != me }.userId
        val last = messageRepo.page(conversationId, null, 1).firstOrNull()
        return ConversationDto(
            id          = conversationId.toString(),
            otherUser   = chatUser(otherId, userRefRepo.findById(otherId)),
            lastMessage = last?.let { it.toDto(decrypt(it, keyRepo.findKey(conversationId, it.keyVersion)).preview()) },
            unread      = last != null && conversationId in messageRepo.unreadConversationIds(me, listOf(conversationId)),
        )
    }

    // conversationId tính được từ 2 userId public (§13.1) → mọi endpoint {id} phải kiểm tra.
    // 404 thay vì 403 để không lộ conversation nào tồn tại.
    private fun requireParticipant(conversationId: UUID, me: UUID) {
        participantRepo.findParticipant(conversationId, me)
            ?: throw NotFoundException("Conversation $conversationId not found")
    }

    private fun decrypt(message: ChatMessage, key: ConversationKey?): String {
        checkNotNull(key) { "Missing data key v${message.keyVersion} for conversation ${message.conversationId}" }
        return ChatCipher.decrypt(
            dataKeys.unwrap(message.conversationId, key.version, key.encryptedDataKey),
            message.nonce,
            message.ciphertext,
            ChatCipher.aad(message.conversationId, message.id, message.keyVersion),
        )
    }

    // Kafka chỉ chở bản mã + DEK đã bọc; websocket-service tự unwrap qua KMS (ADR 0007, plan §6).
    private fun chatMessageEvent(message: ChatMessage, wrappedKey: ByteArray, participantIds: List<UUID>): OutboxEntry {
        val b64 = Base64.getEncoder()
        val event = SocialEvent(
            eventType = EventType.CHAT_MESSAGE,
            payload = mapOf(
                "messageId"       to message.id.toString(),
                "clientMessageId" to message.clientMessageId.toString(),
                "conversationId"  to message.conversationId.toString(),
                "senderId"        to message.senderId.toString(),
                "participantIds"  to participantIds.joinToString(","),
                "keyVersion"      to message.keyVersion.toString(),
                "wrappedKey"      to b64.encodeToString(wrappedKey),
                "nonce"           to b64.encodeToString(message.nonce),
                "ciphertext"      to b64.encodeToString(message.ciphertext),
                "createdAt"       to message.createdAt.toString(),
            ),
        )
        return OutboxEntry().apply {
            aggregateType = "chat"
            // Key Kafka = conversationId → tin cùng conversation vào cùng partition, giữ thứ tự.
            aggregateId   = message.conversationId
            eventType     = event.eventType.name
            payload       = objectMapper.writeValueAsString(event.copy(eventId = id.toString()))
        }
    }

    private fun participant(conversationId: UUID, userId: UUID) = ConversationParticipant().apply {
        this.conversationId = conversationId
        this.userId         = userId
    }

    private fun ChatMessage.toDto(content: String) = MessageDto(
        id              = id.toString(),
        clientMessageId = clientMessageId.toString(),
        conversationId  = conversationId.toString(),
        senderId        = senderId.toString(),
        content         = content,
        createdAt       = createdAt,
    )

    companion object {
        // Chưa có xoay khoá — mọi conversation dùng v1 (plan §12).
        private const val KEY_VERSION = 1
        private const val PREVIEW_CODE_POINTS = 100

        fun directConversationId(a: UUID, b: UUID): UUID {
            val (first, second) = listOf(a.toString(), b.toString()).sorted()
            return UUID.nameUUIDFromBytes("dm:$first:$second".toByteArray(Charsets.UTF_8))
        }

        // Cắt theo code point để không chẻ đôi emoji (surrogate pair).
        internal fun String.preview(): String {
            if (codePointCount(0, length) <= PREVIEW_CODE_POINTS) return this
            return substring(0, offsetByCodePoints(0, PREVIEW_CODE_POINTS)) + "…"
        }
    }
}

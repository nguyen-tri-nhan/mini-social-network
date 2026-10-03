package com.nhan.social.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.chat.crypto.ChatCipher
import com.nhan.social.chat.crypto.DataKeyProvider
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.util.Base64
import java.util.UUID

// Kafka chỉ chở bản mã — giải mã ở đây ngay trước khi push (ADR 0007, messaging-plan §6).
// Không bao giờ log nội dung: lỗi chỉ ghi messageId.
@ApplicationScoped
class ChatEventConsumer(
    private val wsPushService: WsPushService,
    private val dataKeys: DataKeyProvider,
    private val objectMapper: ObjectMapper,
) {
    private val log = Logger.getLogger(ChatEventConsumer::class.java)

    @Incoming("chat-events-in")
    fun consume(message: String) {
        val event = try {
            objectMapper.readValue(message, SocialEvent::class.java)
        } catch (e: Exception) {
            log.errorf(e, "Unreadable chat event")
            return
        }
        if (event.eventType != EventType.CHAT_MESSAGE) return
        try {
            push(event.payload)
        } catch (e: Exception) {
            log.errorf(e, "Failed to push chat message %s", event.payload["messageId"])
        }
    }

    private fun push(payload: Map<String, String>) {
        val browserPayload = decode(payload)
        payload.getValue("participantIds").split(",").filter { it.isNotBlank() }.forEach { userId ->
            val topic = "user_${userId}_chat"
            wsPushService.push(topic, ServerMessage(topic = topic, type = "CHAT_MESSAGE", payload = browserPayload))
        }
    }

    internal fun decode(payload: Map<String, String>): Map<String, String> {
        val b64 = Base64.getDecoder()
        val conversationId = UUID.fromString(payload.getValue("conversationId"))
        val messageId      = UUID.fromString(payload.getValue("messageId"))
        val keyVersion     = payload.getValue("keyVersion").toInt()
        val dek = dataKeys.unwrap(conversationId, keyVersion, b64.decode(payload.getValue("wrappedKey")))
        val content = ChatCipher.decrypt(
            dek,
            b64.decode(payload.getValue("nonce")),
            b64.decode(payload.getValue("ciphertext")),
            ChatCipher.aad(conversationId, messageId, keyVersion),
        )
        return mapOf(
            "id"              to messageId.toString(),
            "clientMessageId" to payload.getValue("clientMessageId"),
            "conversationId"  to conversationId.toString(),
            "senderId"        to payload.getValue("senderId"),
            "content"         to content,
            "createdAt"       to payload.getValue("createdAt"),
        )
    }
}

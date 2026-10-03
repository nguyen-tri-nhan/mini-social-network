package com.nhan.social.ws

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.chat.crypto.ChatCipher
import com.nhan.social.chat.crypto.DataKeyProvider
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

class ChatEventConsumerTest {

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val push = mockk<WsPushService>(relaxUnitFun = true)
    private val dek = ByteArray(32) { 3 }
    private val wrapped = ByteArray(48) { 4 }
    private val dataKeys = mockk<DataKeyProvider> { every { unwrap(any(), 1, any()) } returns dek }
    private val consumer = ChatEventConsumer(push, dataKeys, objectMapper)

    private val conversationId = UUID.randomUUID()
    private val messageId = UUID.randomUUID()
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    private fun event(content: String, aadMessageId: UUID = messageId): String {
        val enc = ChatCipher.encrypt(dek, content, ChatCipher.aad(conversationId, aadMessageId, 1))
        val b64 = Base64.getEncoder()
        return objectMapper.writeValueAsString(SocialEvent(
            eventType = EventType.CHAT_MESSAGE,
            payload = mapOf(
                "messageId" to messageId.toString(),
                "clientMessageId" to UUID.randomUUID().toString(),
                "conversationId" to conversationId.toString(),
                "senderId" to alice.toString(),
                "participantIds" to "$alice,$bob",
                "keyVersion" to "1",
                "wrappedKey" to b64.encodeToString(wrapped),
                "nonce" to b64.encodeToString(enc.nonce),
                "ciphertext" to b64.encodeToString(enc.ciphertext),
                "createdAt" to "2026-10-02T10:00:00Z",
            ),
        ))
    }

    @Test
    fun `decrypts and pushes plaintext to every participant including the sender`() {
        consumer.consume(event("chào Bob"))

        for (userId in listOf(alice, bob)) {
            val msg = slot<ServerMessage>()
            verify { push.push("user_${userId}_chat", capture(msg)) }
            assertEquals("CHAT_MESSAGE", msg.captured.type)
            assertEquals("chào Bob", msg.captured.payload["content"])
            assertEquals(messageId.toString(), msg.captured.payload["id"])
            assertEquals(null, msg.captured.payload["ciphertext"])
        }
    }

    @Test
    fun `ciphertext moved to another message id fails the GCM tag and nothing is pushed`() {
        consumer.consume(event("chào Bob", aadMessageId = UUID.randomUUID()))
        verify(exactly = 0) { push.push(any(), any()) }
    }

    @Test
    fun `other event types are ignored`() {
        consumer.consume(objectMapper.writeValueAsString(SocialEvent(EventType.VOTE_CAST, payload = emptyMap())))
        consumer.consume("not json")
        verify(exactly = 0) { push.push(any(), any()) }
    }
}

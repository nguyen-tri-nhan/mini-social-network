package com.nhan.social.chat.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.chat.crypto.ChatCipher
import com.nhan.social.chat.crypto.DataKey
import com.nhan.social.chat.crypto.DataKeyProvider
import com.nhan.social.chat.entity.ChatMessage
import com.nhan.social.chat.entity.ConversationKey
import com.nhan.social.chat.entity.ConversationParticipant
import com.nhan.social.chat.entity.OutboxEntry
import com.nhan.social.chat.entity.UserRef
import com.nhan.social.chat.repository.ChatMessageRepository
import com.nhan.social.chat.repository.ConversationKeyRepository
import com.nhan.social.chat.repository.ConversationRepository
import com.nhan.social.chat.repository.OutboxRepository
import com.nhan.social.chat.repository.ParticipantRepository
import com.nhan.social.chat.repository.UserRefRepository
import com.nhan.social.chat.service.ChatService.Companion.preview
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.BadRequestException
import com.nhan.social.exception.ConflictException
import com.nhan.social.exception.NotFoundException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import java.util.UUID

class ChatServiceTest {

    private val objectMapper     = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val conversationRepo = mockk<ConversationRepository>()
    private val participantRepo  = mockk<ParticipantRepository>(relaxUnitFun = true)
    private val keyRepo          = mockk<ConversationKeyRepository>(relaxUnitFun = true)
    private val messageRepo      = mockk<ChatMessageRepository>()
    private val userRefRepo      = mockk<UserRefRepository>()
    private val outboxRepo       = mockk<OutboxRepository>(relaxUnitFun = true)
    private val dataKeys         = mockk<DataKeyProvider>()
    private val service = ChatService(
        conversationRepo, participantRepo, keyRepo, messageRepo, userRefRepo, outboxRepo, dataKeys, objectMapper,
    )

    private val me    = UUID.randomUUID()
    private val other = UUID.randomUUID()
    private val conversationId = ChatService.directConversationId(me, other)
    private val dek     = ByteArray(32) { 7 }
    private val wrapped = ByteArray(48) { 9 }
    private val key = ConversationKey().apply {
        this.conversationId = this@ChatServiceTest.conversationId
        version = 1
        encryptedDataKey = wrapped
    }

    private fun participant(userId: UUID) = ConversationParticipant().apply {
        this.conversationId = this@ChatServiceTest.conversationId
        this.userId = userId
    }

    private fun givenConversationWithKey() {
        every { participantRepo.findByConversation(conversationId) } returns listOf(participant(me), participant(other))
        every { keyRepo.findKey(conversationId, 1) } returns key
        every { dataKeys.unwrap(conversationId, 1, wrapped) } returns dek
    }

    @Test
    fun `directConversationId is the same whichever side opens it`() {
        assertEquals(ChatService.directConversationId(me, other), ChatService.directConversationId(other, me))
        assertNotEquals(ChatService.directConversationId(me, other), ChatService.directConversationId(me, UUID.randomUUID()))
    }

    @Test
    fun `openDirect with yourself is rejected`() {
        assertThrows<BadRequestException> { service.openDirect(me, me) }
    }

    @Test
    fun `openDirect with unknown user is 404`() {
        every { userRefRepo.findById(other) } returns null
        assertThrows<NotFoundException> { service.openDirect(me, other) }
    }

    private fun givenOpenable() {
        every { userRefRepo.findById(other) } returns UserRef().apply { id = other; username = "b"; firstname = "B"; lastname = "B" }
        every { participantRepo.findByConversation(conversationId) } returns listOf(participant(me), participant(other))
        every { messageRepo.page(conversationId, null, 1) } returns emptyList()
        every { dataKeys.generate(conversationId, 1) } returns DataKey(dek, wrapped)
    }

    @Test
    fun `openDirect winner creates participants and a KMS data key`() {
        givenOpenable()
        every { conversationRepo.insertIfAbsent(conversationId) } returns true

        val dto = service.openDirect(me, other)

        assertEquals(conversationId.toString(), dto.id)
        assertEquals(other.toString(), dto.otherUser.id)
        verify(exactly = 1) { participantRepo.persist(any<ConversationParticipant>(), any<ConversationParticipant>()) }
        verify(exactly = 1) { keyRepo.persist(match<ConversationKey> { it.encryptedDataKey.contentEquals(wrapped) }) }
    }

    @Test
    fun `openDirect on an existing conversation does not call KMS`() {
        givenOpenable()
        every { conversationRepo.insertIfAbsent(conversationId) } returns false

        assertEquals(conversationId.toString(), service.openDirect(me, other).id)
        verify(exactly = 0) { dataKeys.generate(any(), any()) }
        verify(exactly = 0) { keyRepo.persist(any<ConversationKey>()) }
    }

    @Test
    fun `send by non-participant is 404 and nothing is written`() {
        every { participantRepo.findByConversation(conversationId) } returns listOf(participant(other))
        assertThrows<NotFoundException> { service.send(me, conversationId, UUID.randomUUID(), "hi") }
        verify(exactly = 0) { outboxRepo.emit(any()) }
    }

    @Test
    fun `send stores only ciphertext and the event carries it, decryptable with the message AAD`() {
        givenConversationWithKey()
        val stored = slot<ChatMessage>()
        every { messageRepo.insertIfAbsent(capture(stored)) } returns true

        val dto = service.send(me, conversationId, UUID.randomUUID(), "xin chào 👋")

        assertEquals("xin chào 👋", dto.content)
        val event = slot<OutboxEntry>().also { s -> verify { outboxRepo.emit(capture(s)) } }.captured
        assertEquals("chat", event.aggregateType)
        assertEquals(conversationId, event.aggregateId)
        assertFalse(event.payload.contains("xin chào"))

        val payload = objectMapper.readValue(event.payload, SocialEvent::class.java).also {
            assertEquals(EventType.CHAT_MESSAGE, it.eventType)
        }.payload
        assertEquals(setOf(me.toString(), other.toString()), payload.getValue("participantIds").split(",").toSet())
        assertArrayEquals(wrapped, Base64.getDecoder().decode(payload["wrappedKey"]))

        val m = stored.captured
        assertEquals(m.id.toString(), payload["messageId"])
        val plain = ChatCipher.decrypt(
            dek,
            Base64.getDecoder().decode(payload["nonce"]),
            Base64.getDecoder().decode(payload["ciphertext"]),
            ChatCipher.aad(conversationId, m.id, 1),
        )
        assertEquals("xin chào 👋", plain)
    }

    @Test
    fun `resending the same clientMessageId returns the original message without a new event`() {
        givenConversationWithKey()
        val clientId = UUID.randomUUID()
        val original = slot<ChatMessage>()
        every { messageRepo.insertIfAbsent(capture(original)) } returns true
        val first = service.send(me, conversationId, clientId, "một lần thôi")
        clearMocks(outboxRepo)

        every { messageRepo.insertIfAbsent(any()) } returns false
        every { messageRepo.findBySenderAndClientId(me, clientId) } returns original.captured

        val second = service.send(me, conversationId, clientId, "một lần thôi")

        assertEquals(first.id, second.id)
        assertEquals("một lần thôi", second.content)
        verify(exactly = 0) { outboxRepo.emit(any()) }
    }

    @Test
    fun `clientMessageId already used in another conversation is 409`() {
        givenConversationWithKey()
        val clientId = UUID.randomUUID()
        every { messageRepo.insertIfAbsent(any()) } returns false
        every { messageRepo.findBySenderAndClientId(me, clientId) } returns ChatMessage().apply {
            id = UUID.randomUUID(); conversationId = UUID.randomUUID(); senderId = me; clientMessageId = clientId
            nonce = ByteArray(12); ciphertext = ByteArray(16)
        }
        assertThrows<ConflictException> { service.send(me, conversationId, clientId, "hi") }
    }

    @Test
    fun `markRead rejects a message from another conversation`() {
        every { participantRepo.findParticipant(conversationId, me) } returns participant(me)
        val foreign = ChatMessage().apply {
            id = UUID.randomUUID(); conversationId = UUID.randomUUID(); senderId = other; clientMessageId = UUID.randomUUID()
            nonce = ByteArray(12); ciphertext = ByteArray(16)
        }
        every { messageRepo.findById(foreign.id) } returns foreign

        assertThrows<NotFoundException> { service.markRead(me, conversationId, foreign.id) }
        verify(exactly = 0) { participantRepo.advanceLastRead(any(), any(), any()) }
    }

    @Test
    fun `preview cuts by code point and never splits an emoji`() {
        val text = "a".repeat(99) + "😀😀"
        val p = text.preview()
        assertEquals("a".repeat(99) + "😀…", p)
        assertEquals("ngắn", "ngắn".preview())
    }
}

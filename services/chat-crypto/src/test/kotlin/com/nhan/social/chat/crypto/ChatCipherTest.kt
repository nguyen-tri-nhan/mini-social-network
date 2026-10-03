package com.nhan.social.chat.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.AEADBadTagException

class ChatCipherTest {

    private fun newKey() = ByteArray(32).also(SecureRandom()::nextBytes)

    private val key = newKey()
    private val conversationId = UUID.randomUUID()
    private val messageId = UUID.randomUUID()
    private val aad = ChatCipher.aad(conversationId, messageId, 1)

    @Test
    fun `round trip keeps unicode content intact`() {
        val text = "Xin chào 👋 — tin nhắn có dấu"
        val enc = ChatCipher.encrypt(key, text, aad)
        assertEquals(text, ChatCipher.decrypt(key, enc.nonce, enc.ciphertext, aad))
    }

    @Test
    fun `ciphertext copied to another message fails authentication`() {
        val enc = ChatCipher.encrypt(key, "secret", aad)
        val otherAad = ChatCipher.aad(conversationId, UUID.randomUUID(), 1)
        assertThrows<AEADBadTagException> { ChatCipher.decrypt(key, enc.nonce, enc.ciphertext, otherAad) }
    }

    @Test
    fun `wrong key fails authentication`() {
        val enc = ChatCipher.encrypt(key, "secret", aad)
        assertThrows<AEADBadTagException> { ChatCipher.decrypt(newKey(), enc.nonce, enc.ciphertext, aad) }
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val enc = ChatCipher.encrypt(key, "secret", aad)
        enc.ciphertext[0] = (enc.ciphertext[0].toInt() xor 1).toByte()
        assertThrows<AEADBadTagException> { ChatCipher.decrypt(key, enc.nonce, enc.ciphertext, aad) }
    }

    @Test
    fun `every encryption uses a fresh nonce`() {
        val a = ChatCipher.encrypt(key, "same", aad)
        val b = ChatCipher.encrypt(key, "same", aad)
        assertFalse(a.nonce.contentEquals(b.nonce))
        assertFalse(a.ciphertext.contentEquals(b.ciphertext))
    }
}

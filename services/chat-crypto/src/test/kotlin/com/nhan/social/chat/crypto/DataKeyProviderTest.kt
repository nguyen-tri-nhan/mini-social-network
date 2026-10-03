package com.nhan.social.chat.crypto

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DataKeySpec
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DecryptResponse
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse
import java.util.UUID

class DataKeyProviderTest {

    private val plain   = ByteArray(32) { 1 }
    private val wrapped = ByteArray(64) { 2 }
    private val conversationId = UUID.randomUUID()

    private val kms = mockk<KmsClient> {
        every { generateDataKey(any<GenerateDataKeyRequest>()) } returns GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plain))
            .ciphertextBlob(SdkBytes.fromByteArray(wrapped))
            .build()
        every { decrypt(any<DecryptRequest>()) } returns DecryptResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plain))
            .build()
    }
    private var now = 1_000_000L
    private val provider = DataKeyProvider(kms, "alias/social-chat").also { it.clock = { now } }

    @Test
    fun `generate asks KMS for an AES-256 key bound to the conversation`() {
        val req = slot<GenerateDataKeyRequest>()
        every { kms.generateDataKey(capture(req)) } returns GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(plain)).ciphertextBlob(SdkBytes.fromByteArray(wrapped)).build()

        val key = provider.generate(conversationId, 1)

        assertEquals("alias/social-chat", req.captured.keyId())
        assertEquals(DataKeySpec.AES_256, req.captured.keySpec())
        assertEquals(mapOf("conversationId" to conversationId.toString()), req.captured.encryptionContext())
        assertArrayEquals(plain, key.plaintext)
        assertArrayEquals(wrapped, key.wrapped)
    }

    @Test
    fun `unwrap passes the conversation as encryption context`() {
        val req = slot<DecryptRequest>()
        every { kms.decrypt(capture(req)) } returns DecryptResponse.builder().plaintext(SdkBytes.fromByteArray(plain)).build()

        provider.unwrap(conversationId, 1, wrapped)

        assertEquals(mapOf("conversationId" to conversationId.toString()), req.captured.encryptionContext())
        assertArrayEquals(wrapped, req.captured.ciphertextBlob().asByteArray())
    }

    @Test
    fun `unwrap is served from cache within 5 minutes`() {
        provider.unwrap(conversationId, 1, wrapped)
        now += 4 * 60_000
        provider.unwrap(conversationId, 1, wrapped)

        verify(exactly = 1) { kms.decrypt(any<DecryptRequest>()) }
    }

    @Test
    fun `unwrap calls KMS again after the cache expires`() {
        provider.unwrap(conversationId, 1, wrapped)
        now += 5 * 60_000 + 1
        provider.unwrap(conversationId, 1, wrapped)

        verify(exactly = 2) { kms.decrypt(any<DecryptRequest>()) }
    }

    @Test
    fun `a freshly generated key is cached so the first message needs no decrypt call`() {
        provider.generate(conversationId, 1)
        provider.unwrap(conversationId, 1, wrapped)

        verify(exactly = 0) { kms.decrypt(any<DecryptRequest>()) }
    }

    @Test
    fun `different key versions are cached separately`() {
        provider.unwrap(conversationId, 1, wrapped)
        provider.unwrap(conversationId, 2, wrapped)

        verify(exactly = 2) { kms.decrypt(any<DecryptRequest>()) }
    }
}

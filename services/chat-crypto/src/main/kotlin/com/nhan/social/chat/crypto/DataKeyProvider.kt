package com.nhan.social.chat.crypto

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DataKeySpec
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DataKey(val plaintext: ByteArray, val wrapped: ByteArray)

// Envelope encryption: 1 DEK / conversation, bọc bởi CMK trong KMS; DEK bản thường chỉ
// nằm trong RAM, cache 5 phút — xem messaging-plan.md §12.2, ADR 0007.
@ApplicationScoped
class DataKeyProvider(
    private val kms: KmsClient,
    @ConfigProperty(name = "app.chat.kms-key-id") private val keyId: String,
) {
    internal var clock: () -> Long = System::currentTimeMillis

    private class Cached(val key: ByteArray, val expiresAt: Long)
    private val cache = ConcurrentHashMap<String, Cached>()

    fun generate(conversationId: UUID, version: Int): DataKey {
        val res = kms.generateDataKey(
            GenerateDataKeyRequest.builder()
                .keyId(keyId)
                .keySpec(DataKeySpec.AES_256)
                .encryptionContext(context(conversationId))
                .build(),
        )
        val key = DataKey(res.plaintext().asByteArray(), res.ciphertextBlob().asByteArray())
        remember(conversationId, version, key.plaintext)
        return key
    }

    fun unwrap(conversationId: UUID, version: Int, wrapped: ByteArray): ByteArray {
        cache[cacheKey(conversationId, version)]?.takeIf { it.expiresAt > clock() }?.let { return it.key }

        // EncryptionContext phải khớp lúc generate — KMS từ chối nếu ai đó tráo DEK
        // của conversation khác vào hàng này.
        val plaintext = kms.decrypt(
            DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(wrapped))
                .encryptionContext(context(conversationId))
                .build(),
        ).plaintext().asByteArray()
        remember(conversationId, version, plaintext)
        return plaintext
    }

    private fun remember(conversationId: UUID, version: Int, key: ByteArray) {
        val now = clock()
        if (cache.size > MAX_ENTRIES) cache.entries.removeIf { it.value.expiresAt <= now }
        cache[cacheKey(conversationId, version)] = Cached(key, now + TTL_MILLIS)
    }

    private fun cacheKey(conversationId: UUID, version: Int) = "$conversationId:$version"
    private fun context(conversationId: UUID) = mapOf("conversationId" to conversationId.toString())

    companion object {
        private const val TTL_MILLIS = 5 * 60_000L
        private const val MAX_ENTRIES = 10_000
    }
}

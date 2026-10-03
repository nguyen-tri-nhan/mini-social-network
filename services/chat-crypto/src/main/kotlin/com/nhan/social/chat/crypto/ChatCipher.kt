package com.nhan.social.chat.crypto

import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class Encrypted(val nonce: ByteArray, val ciphertext: ByteArray)

// AES-256-GCM cho nội dung tin nhắn — xem messaging-plan.md §12.2.
object ChatCipher {
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    // Gắn bản mã với đúng hàng của nó: chép ciphertext sang tin/conversation khác
    // → giải mã ném AEADBadTagException thay vì ra nội dung sai.
    fun aad(conversationId: UUID, messageId: UUID, keyVersion: Int): ByteArray =
        "$conversationId|$messageId|$keyVersion".toByteArray(Charsets.UTF_8)

    fun encrypt(key: ByteArray, plaintext: String, aad: ByteArray): Encrypted {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return Encrypted(nonce, cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}

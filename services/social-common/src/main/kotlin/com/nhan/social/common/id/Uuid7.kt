package com.nhan.social.common.id

import java.security.SecureRandom
import java.util.UUID

// UUIDv7 (RFC 9562): 48 bit Unix ms · version 7 · 12 bit random · variant 10 · 62 bit random.
// Postgres 15 chưa có uuidv7() (có từ 18) nên sinh ở app. Postgres so sánh uuid theo byte →
// sắp theo thời gian; trong Kotlin đừng dùng UUID.compareTo (so sánh signed long) — so sánh
// toString() mới khớp thứ tự byte.
object Uuid7 {
    private val random = SecureRandom()

    fun next(nowMillis: Long = System.currentTimeMillis()): UUID {
        val msb = (nowMillis shl 16) or (0x7L shl 12) or (random.nextInt() and 0xFFF).toLong()
        val lsb = (random.nextLong() and 0x3FFF_FFFF_FFFF_FFFFL) or Long.MIN_VALUE
        return UUID(msb, lsb)
    }

    fun timestampMillis(uuid: UUID): Long = uuid.mostSignificantBits ushr 16
}

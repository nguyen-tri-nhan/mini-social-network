package com.nhan.social.common.id

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Uuid7Test {

    @Test
    fun `has version 7 and IETF variant`() {
        val id = Uuid7.next()
        assertEquals(7, id.version())
        assertEquals(2, id.variant())   // java.util.UUID: 2 = variant bits 10 (RFC 9562)
    }

    @Test
    fun `embeds the timestamp`() {
        val now = 1_790_000_000_123L
        assertEquals(now, Uuid7.timestampMillis(Uuid7.next(now)))
    }

    @Test
    fun `string order follows time order`() {
        val earlier = Uuid7.next(1_790_000_000_000L).toString()
        val later   = Uuid7.next(1_790_000_000_001L).toString()
        assertTrue(earlier < later)
    }

    @Test
    fun `ids generated in the same millisecond are distinct`() {
        val ids = (1..1000).map { Uuid7.next(1_790_000_000_000L) }.toSet()
        assertEquals(1000, ids.size)
    }
}

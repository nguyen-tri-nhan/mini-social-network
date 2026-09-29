package com.nhan.social.exception.filter

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InternalAuthFilterTest {

    private val filter = InternalAuthFilter().apply { secretKey = "s3cret" }

    private fun request(path: String, key: String?): ContainerRequestContext = mockk(relaxed = true) {
        every { uriInfo.path } returns path
        every { getHeaderString("X-Service-Secret-Key") } returns key
    }

    @Test
    fun `internal path with leading slash and no key is rejected`() {
        val ctx = request("/internal/articles/1", key = null)
        val response = slot<Response>()

        filter.filter(ctx)

        verify { ctx.abortWith(capture(response)) }
        assertEquals(401, response.captured.status)
    }

    @Test
    fun `internal path with wrong key is rejected`() {
        val ctx = request("/internal/articles/1", key = "wrong")

        filter.filter(ctx)

        verify { ctx.abortWith(any()) }
    }

    @Test
    fun `internal path with correct key passes`() {
        val ctx = request("/internal/articles/1", key = "s3cret")

        filter.filter(ctx)

        verify(exactly = 0) { ctx.abortWith(any()) }
    }

    @Test
    fun `public api path is not checked`() {
        val ctx = request("/api/articles", key = null)

        filter.filter(ctx)

        verify(exactly = 0) { ctx.abortWith(any()) }
    }

    @Test
    fun `path that merely starts with internal prefix is not treated as internal`() {
        val ctx = request("/internalfoo", key = null)

        filter.filter(ctx)

        verify(exactly = 0) { ctx.abortWith(any()) }
    }
}

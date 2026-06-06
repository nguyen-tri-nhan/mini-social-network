package com.nhan.social.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhan.social.auth.dto.SignInRequest
import com.nhan.social.auth.dto.SignUpRequest
import com.nhan.social.auth.entity.Credentials
import com.nhan.social.auth.entity.OutboxEntry
import com.nhan.social.auth.repository.CredentialsRepository
import com.nhan.social.auth.repository.OutboxRepository
import com.nhan.social.auth.security.JwtService
import com.nhan.social.exception.ConflictException
import com.nhan.social.exception.UnauthorizedException
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AuthServiceTest {

    private val repo        = mockk<CredentialsRepository>()
    private val outboxRepo  = mockk<OutboxRepository>()
    private val jwtService  = mockk<JwtService>()
    private val service     = AuthService(repo, outboxRepo, jwtService, jacksonObjectMapper().registerModule(JavaTimeModule()))

    private val request = SignUpRequest(
        username  = "nhan",
        email     = "nhan@example.com",
        password  = "password123",
        firstname = "Nhan",
        lastname  = "Nguyen",
    )

    // ── U-01 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-01 signup valid request - credentials and outbox persisted, JWT returned`() {
        every { repo.existsByUsername(any()) } returns false
        every { repo.existsByEmail(any()) }    returns false
        every { repo.persist(any<Credentials>()) } just Runs
        every { outboxRepo.persist(any<OutboxEntry>()) } just Runs
        every { jwtService.generateToken(any(), "nhan", any()) } returns "jwt-token"

        val result = service.signup(request)

        assertEquals("jwt-token", result.accessToken)
        assertEquals("nhan", result.username)
        assertNotNull(result.userId)
        verify { repo.persist(any<Credentials>()) }
        verify { outboxRepo.persist(any<OutboxEntry>()) }
    }

    // ── U-02 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-02 signup duplicate username - throws ConflictException`() {
        every { repo.existsByUsername("nhan") } returns true

        assertThrows<ConflictException> { service.signup(request) }
        verify(exactly = 0) { repo.persist(any<Credentials>()) }
    }

    // ── U-03 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-03 signup duplicate email - throws ConflictException`() {
        every { repo.existsByUsername(any()) }         returns false
        every { repo.existsByEmail("nhan@example.com") } returns true

        assertThrows<ConflictException> { service.signup(request) }
    }

    // ── U-04 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-04 signin correct credentials - returns JWT`() {
        val hash = BCrypt.withDefaults().hashToString(4, "password123".toCharArray())
        val creds = Credentials().apply {
            userId       = UUID.randomUUID()
            username     = "nhan"
            passwordHash = hash
        }
        every { repo.findByIdentifier("nhan") } returns creds
        every { jwtService.generateToken(any(), "nhan", any()) } returns "jwt-token"

        val result = service.signin(SignInRequest("nhan", "password123"))

        assertEquals("jwt-token", result.accessToken)
        assertEquals("nhan11", result.username)
    }

    // ── U-05 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-05 signin wrong password - throws UnauthorizedException`() {
        val hash = BCrypt.withDefaults().hashToString(4, "correct".toCharArray())
        val creds = Credentials().apply {
            userId       = UUID.randomUUID()
            username     = "nhan"
            passwordHash = hash
        }
        every { repo.findByIdentifier(any()) } returns creds

        assertThrows<UnauthorizedException> {
            service.signin(SignInRequest("nhan", "wrong"))
        }
    }

    // ── U-06 ──────────────────────────────────────────────────────────────────

    @Test
    fun `U-06 signin unknown identifier - throws UnauthorizedException`() {
        every { repo.findByIdentifier(any()) } returns null

        assertThrows<UnauthorizedException> {
            service.signin(SignInRequest("ghost", "password"))
        }
    }
}

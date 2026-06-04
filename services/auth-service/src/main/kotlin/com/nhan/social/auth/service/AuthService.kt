package com.nhan.social.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhan.social.auth.dto.AuthResponse
import com.nhan.social.auth.dto.SignInRequest
import com.nhan.social.auth.dto.SignUpRequest
import com.nhan.social.auth.entity.Credentials
import com.nhan.social.auth.entity.OutboxEntry
import com.nhan.social.auth.repository.CredentialsRepository
import com.nhan.social.auth.repository.OutboxRepository
import com.nhan.social.auth.security.JwtService
import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.ConflictException
import com.nhan.social.exception.UnauthorizedException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class AuthService(
    private val repo: CredentialsRepository,
    private val outboxRepo: OutboxRepository,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun signup(request: SignUpRequest): AuthResponse {
        if (repo.existsByUsername(request.username))
            throw ConflictException("Username '${request.username}' is already taken")
        if (repo.existsByEmail(request.email))
            throw ConflictException("Email '${request.email}' is already registered")

        val userId = UUID.randomUUID()
        val credentials = Credentials().apply {
            this.id           = UUID.randomUUID()
            this.username     = request.username
            this.email        = request.email
            this.passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
            this.userId       = userId
            this.createdAt    = Instant.now()
        }
        repo.persist(credentials)

        outboxRepo.persist(outbox(
            aggregateType = "user",
            aggregateId   = userId,
            event = SocialEvent(
                eventType = EventType.USER_UPDATED,
                payload = mapOf(
                    "userId"    to userId.toString(),
                    "username"  to request.username,
                    "email"     to request.email,
                    "firstname" to request.firstname,
                    "lastname"  to request.lastname,
                    "action"    to "CREATED",
                ),
            ),
        ))

        val token = jwtService.generateToken(userId.toString(), request.username, setOf("ROLE_USER"))
        return AuthResponse(accessToken = token, userId = userId.toString(), username = request.username)
    }

    private fun outbox(aggregateType: String, aggregateId: UUID, event: SocialEvent): OutboxEntry =
        OutboxEntry().apply {
            this.aggregateType = aggregateType
            this.aggregateId   = aggregateId
            this.eventType     = event.eventType.name
            this.payload       = objectMapper.writeValueAsString(event.copy(eventId = this.id.toString()))
        }

    fun signin(request: SignInRequest): AuthResponse {
        val credentials = repo.findByIdentifier(request.identifier)
            ?: throw UnauthorizedException("Invalid credentials")

        val verified = BCrypt.verifyer().verify(request.password.toCharArray(), credentials.passwordHash)
        if (!verified.verified) throw UnauthorizedException("Invalid credentials")

        val token = jwtService.generateToken(
            credentials.userId.toString(),
            credentials.username,
            setOf("ROLE_USER"),
        )
        return AuthResponse(
            accessToken = token,
            userId      = credentials.userId.toString(),
            username    = credentials.username,
        )
    }
}

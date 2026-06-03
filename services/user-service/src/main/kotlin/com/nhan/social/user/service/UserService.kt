package com.nhan.social.user.service

import com.nhan.social.common.event.EventType
import com.nhan.social.common.event.SocialEvent
import com.nhan.social.exception.NotFoundException
import com.nhan.social.user.dto.UpdateProfileRequest
import com.nhan.social.user.dto.UserProfileDto
import com.nhan.social.user.dto.toDto
import com.nhan.social.user.entity.UserProfile
import com.nhan.social.user.repository.UserProfileRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class UserService(
    private val repo: UserProfileRepository,
    private val redis: RedisDataSource,
    private val objectMapper: ObjectMapper,
) {
    private val valueCommands get() = redis.value(String::class.java)
    private val CACHE_TTL = Duration.ofMinutes(10)

    fun getById(id: UUID): UserProfileDto =
        repo.findById(id)?.toDto() ?: throw NotFoundException("User $id not found")

    fun getCachedById(id: UUID): UserProfileDto {
        val key = "user:$id:profile"
        val cached = valueCommands.get(key)
        if (cached != null) return objectMapper.readValue(cached, UserProfileDto::class.java)

        val profile = getById(id)
        valueCommands.setex(key, CACHE_TTL.seconds, objectMapper.writeValueAsString(profile))
        return profile
    }

    @Transactional
    fun createFromEvent(event: SocialEvent) {
        val payload = event.payload
        val userId = UUID.fromString(payload["userId"] ?: return)
        if (repo.findById(userId) != null) return

        repo.persist(UserProfile().apply {
            id = userId
            username = payload["username"] ?: ""
            email = payload["email"] ?: ""
            firstname = payload["firstname"] ?: ""
            lastname = payload["lastname"] ?: ""
            createdAt = Instant.now()
            updatedAt = Instant.now()
        })
    }

    @Transactional
    fun update(id: UUID, request: UpdateProfileRequest): UserProfileDto {
        val profile = repo.findById(id) ?: throw NotFoundException("User $id not found")
        request.firstname?.let { profile.firstname = it }
        request.lastname?.let { profile.lastname = it }
        request.avatarUrl?.let { profile.avatarUrl = it }
        profile.updatedAt = Instant.now()
        valueCommands.getdel("user:$id:profile")  // invalidate cache
        return profile.toDto()
    }
}

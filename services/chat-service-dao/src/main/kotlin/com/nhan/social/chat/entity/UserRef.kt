package com.nhan.social.chat.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// Materialized view của user-service qua social.user — y hệt interaction-service (ADR 0006).
@Entity
@Table(name = "user_ref")
class UserRef : PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    lateinit var id: UUID

    @Column(nullable = false, length = 50)
    lateinit var username: String

    @Column(nullable = false, length = 100)
    lateinit var firstname: String

    @Column(nullable = false, length = 100)
    lateinit var lastname: String

    @Column(name = "avatar_url")
    var avatarUrl: String? = null

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
}

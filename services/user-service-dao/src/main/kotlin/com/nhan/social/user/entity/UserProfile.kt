package com.nhan.social.user.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_profile")
class UserProfile : PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(unique = true, nullable = false, length = 50)
    lateinit var username: String

    @Column(unique = true, nullable = false)
    lateinit var email: String

    @Column(nullable = false, length = 100)
    lateinit var firstname: String

    @Column(nullable = false, length = 100)
    lateinit var lastname: String

    @Column(name = "avatar_url")
    var avatarUrl: String? = null

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
}

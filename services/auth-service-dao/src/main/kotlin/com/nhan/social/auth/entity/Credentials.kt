package com.nhan.social.auth.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "credentials")
class Credentials : PanacheEntityBase() {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(unique = true, nullable = false, length = 50)
    lateinit var username: String

    @Column(unique = true, nullable = false)
    lateinit var email: String

    @Column(name = "password_hash", nullable = false)
    lateinit var passwordHash: String

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    var userId: UUID = UUID.randomUUID()

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

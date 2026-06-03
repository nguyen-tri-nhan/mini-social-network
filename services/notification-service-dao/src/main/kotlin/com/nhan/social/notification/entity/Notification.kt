package com.nhan.social.notification.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification")
class Notification : PanacheEntityBase() {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(nullable = false, length = 30)
    lateinit var type: String

    @Column(name = "actor_id", nullable = false, columnDefinition = "uuid")
    lateinit var actorId: UUID

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    lateinit var ownerId: UUID

    @Column(name = "article_id", columnDefinition = "uuid")
    var articleId: UUID? = null

    @Column(nullable = false)
    var seen: Boolean = false

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

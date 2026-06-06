package com.nhan.social.interaction.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox")
class OutboxEntry : PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(name = "aggregate_type", nullable = false, length = 50)
    lateinit var aggregateType: String   // "comment" | "vote"

    @Column(name = "aggregate_id", nullable = false, columnDefinition = "uuid")
    lateinit var aggregateId: UUID

    @Column(name = "event_type", nullable = false, length = 50)
    lateinit var eventType: String

    @Column(nullable = false, columnDefinition = "text")
    lateinit var payload: String         // SocialEvent serialized as JSON

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

package com.nhan.social.interaction.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "vote",
    uniqueConstraints = [UniqueConstraint(
        name = "uq_vote_user_target",
        columnNames = ["user_id", "target_id", "target_type"],
    )]
)
class Vote : PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var value: Short = 0

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    lateinit var userId: UUID

    @Column(name = "target_id", nullable = false, columnDefinition = "uuid")
    lateinit var targetId: UUID

    @Column(name = "target_type", nullable = false, length = 20)
    lateinit var targetType: String

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

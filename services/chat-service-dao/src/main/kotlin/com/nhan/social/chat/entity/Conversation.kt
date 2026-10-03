package com.nhan.social.chat.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// 1:1: id tất định từ cặp user (messaging-plan.md §13.1) — app gán, không có default.
@Entity
@Table(name = "conversation")
class Conversation : PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    lateinit var id: UUID

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

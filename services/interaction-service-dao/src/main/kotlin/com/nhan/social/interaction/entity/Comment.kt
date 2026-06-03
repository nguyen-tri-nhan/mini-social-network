package com.nhan.social.interaction.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "comment")
class Comment : PanacheEntityBase() {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(columnDefinition = "TEXT", nullable = false)
    lateinit var description: String

    @Column(name = "article_id", nullable = false, columnDefinition = "uuid")
    lateinit var articleId: UUID

    @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
    lateinit var authorId: UUID

    @Column(nullable = false)
    var visible: Boolean = true

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
}

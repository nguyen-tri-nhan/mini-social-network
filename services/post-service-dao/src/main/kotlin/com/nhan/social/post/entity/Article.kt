package com.nhan.social.post.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "article")
class Article : PanacheEntityBase() {

    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "image_url")
    var imageUrl: String? = null

    @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
    lateinit var authorId: UUID

    @Column(nullable = false)
    var visible: Boolean = true

    @Column(name = "vote_count", nullable = false)
    var voteCount: Int = 0

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
}

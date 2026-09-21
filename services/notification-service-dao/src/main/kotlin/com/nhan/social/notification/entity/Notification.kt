package com.nhan.social.notification.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification")
class Notification : PanacheEntityBase {

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

    // Đóng băng tên actor tại thời điểm tạo notification — không join lại
    // user_ref như Comment (không sai, notification vốn là bản ghi tại 1
    // thời điểm). Xem specs/decisions/0006.
    @Column(name = "actor_username", length = 50)
    var actorUsername: String? = null

    @Column(name = "actor_firstname", length = 100)
    var actorFirstname: String? = null

    @Column(name = "actor_lastname", length = 100)
    var actorLastname: String? = null

    @Column(name = "actor_avatar_url")
    var actorAvatarUrl: String? = null

    @Column(nullable = false)
    var seen: Boolean = false

    @Column(name = "event_id", columnDefinition = "uuid", unique = true)
    var eventId: UUID? = null   // outbox event ID — nullable for backward compat

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}

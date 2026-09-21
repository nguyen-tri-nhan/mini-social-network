package com.nhan.social.interaction.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// Materialized view của user-service, cập nhật qua consume social.user —
// tránh gọi mạng mỗi lần cần tên tác giả. Xem specs/decisions/0006.
@Entity
@Table(name = "user_ref")
class UserRef : PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    lateinit var id: UUID   // userId

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

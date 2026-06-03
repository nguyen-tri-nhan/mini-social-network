package com.nhan.social.notification.repository

import com.nhan.social.notification.entity.Notification
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class NotificationRepository : PanacheRepositoryBase<Notification, UUID> {

    fun findByOwner(ownerId: UUID, page: Int, size: Int): List<Notification> =
        find("ownerId = ?1", Sort.by("createdAt").descending(), ownerId)
            .page(Page.of(page, size))
            .list()

    fun countUnread(ownerId: UUID): Long =
        count("ownerId = ?1 and seen = false", ownerId)

    fun markAllSeen(ownerId: UUID) {
        update("seen = true WHERE ownerId = ?1 and seen = false", ownerId)
    }
}

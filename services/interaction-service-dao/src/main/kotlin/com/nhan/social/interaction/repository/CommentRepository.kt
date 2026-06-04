package com.nhan.social.interaction.repository

import com.nhan.social.interaction.entity.Comment
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class CommentRepository : PanacheRepositoryBase<Comment, UUID> {

    fun findByTarget(targetId: UUID, targetType: String, page: Int, size: Int): List<Comment> =
        find(
            "targetId = ?1 and targetType = ?2 and visible = true",
            Sort.by("createdAt").ascending(),
            targetId, targetType,
        ).page(Page.of(page, size)).list()

    fun countByTarget(targetId: UUID, targetType: String): Long =
        count("targetId = ?1 and targetType = ?2 and visible = true", targetId, targetType)
}

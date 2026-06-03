package com.nhan.social.interaction.repository

import com.nhan.social.interaction.entity.Comment
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class CommentRepository : PanacheRepositoryBase<Comment, UUID> {

    fun findByArticle(articleId: UUID, page: Int, size: Int): List<Comment> =
        find("articleId = ?1 and visible = true", Sort.by("createdAt").ascending(), articleId)
            .page(Page.of(page, size))
            .list()

    fun countByArticle(articleId: UUID): Long =
        count("articleId = ?1 and visible = true", articleId)
}

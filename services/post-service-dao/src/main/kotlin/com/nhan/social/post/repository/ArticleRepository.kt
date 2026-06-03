package com.nhan.social.post.repository

import com.nhan.social.post.entity.Article
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class ArticleRepository : PanacheRepositoryBase<Article, UUID> {

    fun findVisible(page: Int, size: Int): List<Article> =
        find("visible = true", Sort.by("createdAt").descending())
            .page(Page.of(page, size))
            .list()

    fun countVisible(): Long = count("visible = true")

    fun findFiltered(hql: String, params: Map<String, Any>, sort: Sort, page: Int, size: Int): List<Article> =
        find(hql, sort, params)
            .page(Page.of(page, size))
            .list()

    fun countFiltered(hql: String, params: Map<String, Any>): Long =
        count(hql, params)

    fun updateCounters(id: UUID, voteCount: Long, commentCount: Long) {
        update(
            "voteCount = ?1, commentCount = ?2, updatedAt = ?3 WHERE id = ?4",
            voteCount, commentCount, Instant.now(), id,
        )
    }
}

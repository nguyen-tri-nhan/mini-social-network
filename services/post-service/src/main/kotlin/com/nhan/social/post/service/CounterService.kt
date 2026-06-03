package com.nhan.social.post.service

import com.nhan.social.post.repository.ArticleRepository
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.util.UUID

@ApplicationScoped
class CounterService(
    private val redis: RedisDataSource,   // blocking client — safe on @Scheduled worker thread
    private val articleRepo: ArticleRepository,
) {
    private val log = Logger.getLogger(CounterService::class.java)
    private val valueCommands get() = redis.value(String::class.java)

    fun incrementComment(articleId: String) {
        try {
            valueCommands.incr("article:$articleId:comment_count")
        } catch (e: Exception) {
            log.errorf(e, "Failed to increment comment count for article %s", articleId)
        }
    }

    fun incrementVote(articleId: String, delta: Long) {
        try {
            if (delta >= 0) valueCommands.incrby("article:$articleId:vote_count", delta)
            else valueCommands.decrby("article:$articleId:vote_count", -delta)
        } catch (e: Exception) {
            log.errorf(e, "Failed to update vote count for article %s", articleId)
        }
    }

    @Scheduled(every = "30s")
    @Transactional
    fun flushCounters() {
        try {
            val keys = redis.key(String::class.java).keys("article:*:comment_count")
            keys.forEach { key ->
                val parts = key.split(":")
                if (parts.size < 3) return@forEach
                val articleId = parts[1]
                val commentCount = valueCommands.get(key)?.toLongOrNull() ?: return@forEach
                val voteCount = valueCommands.get("article:$articleId:vote_count")?.toLongOrNull() ?: 0L
                articleRepo.updateCounters(UUID.fromString(articleId), voteCount, commentCount)
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to flush counters to DB")
        }
    }
}

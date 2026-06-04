package com.nhan.social.post.service

import com.nhan.social.post.repository.ArticleRepository
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Duration
import java.util.UUID

@ApplicationScoped
class CounterService(
    private val redis: RedisDataSource,
    private val articleRepo: ArticleRepository,
) {
    private val log = Logger.getLogger(CounterService::class.java)
    private val value get() = redis.value(String::class.java)
    private val key   get() = redis.key(String::class.java)

    fun incrementComment(articleId: String, eventId: String?) {
        if (!tryClaimEvent(eventId)) return
        try {
            value.incr("article:$articleId:comment_count")
        } catch (e: Exception) {
            log.errorf(e, "Failed to increment comment count for article %s", articleId)
        }
    }

    fun incrementVote(articleId: String, delta: Long, eventId: String?) {
        if (!tryClaimEvent(eventId)) return
        try {
            if (delta >= 0) value.incrby("article:$articleId:vote_count", delta)
            else            value.decrby("article:$articleId:vote_count", -delta)
        } catch (e: Exception) {
            log.errorf(e, "Failed to update vote count for article %s", articleId)
        }
    }

    fun readLiveCounts(articleId: String): Pair<Int, Int> {
        val votes    = value.get("article:$articleId:vote_count")?.toIntOrNull()    ?: -1
        val comments = value.get("article:$articleId:comment_count")?.toIntOrNull() ?: -1
        return votes to comments
    }

    /** Called by CounterFlushJob in post-consumer every 30s. */
    @Transactional
    fun flushToDb() {
        try {
            val keys = key.keys("article:*:comment_count")
            keys.forEach { k ->
                val parts = k.split(":")
                if (parts.size < 3) return@forEach
                val articleId    = parts[1]
                val commentCount = value.get(k)?.toLongOrNull()                              ?: return@forEach
                val voteCount    = value.get("article:$articleId:vote_count")?.toLongOrNull() ?: 0L
                articleRepo.updateCounters(UUID.fromString(articleId), voteCount, commentCount)
            }
        } catch (e: Exception) {
            log.errorf(e, "Failed to flush counters to DB")
        }
    }

    /**
     * Idempotency guard via Redis SETNX.
     * Returns true if this eventId has not been processed before (safe to proceed).
     * Returns false if the event was already processed (skip).
     * Null eventId always returns true (no dedup — old-format events).
     */
    private fun tryClaimEvent(eventId: String?): Boolean {
        if (eventId == null) return true
        val dedupKey = "counter:processed:$eventId"
        val claimed  = value.setnx(dedupKey, "1")
        if (claimed) key.expire(dedupKey, Duration.ofHours(24))
        return claimed
    }
}

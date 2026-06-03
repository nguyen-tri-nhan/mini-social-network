package com.nhan.social.post.rsql

import com.nhan.social.common.rsql.RsqlField
import com.nhan.social.common.rsql.RsqlQuerySpec
import com.nhan.social.common.rsql.parseRsql
import com.nhan.social.common.rsql.parseRsqlSort
import com.nhan.social.exception.BadRequestException
import io.quarkus.panache.common.Sort
import java.time.Instant
import java.util.UUID

private val FILTER_FIELDS = mapOf(
    "authorId"  to RsqlField { UUID.fromString(it) },
    "createdAt" to RsqlField { Instant.parse(it) },
)

private val SORT_FIELDS = setOf("createdAt", "voteCount", "commentCount")

fun parseArticleFilter(rsql: String): RsqlQuerySpec = try {
    parseRsql(rsql, FILTER_FIELDS, baseCondition = "visible = true")
} catch (e: IllegalArgumentException) {
    throw BadRequestException(e.message ?: "Invalid filter")
}

fun parseArticleSort(sort: String?): Sort = try {
    parseRsqlSort(sort, SORT_FIELDS, defaultField = "createdAt").let {
        if (it.ascending) Sort.by(it.field).ascending() else Sort.by(it.field).descending()
    }
} catch (e: IllegalArgumentException) {
    throw BadRequestException(e.message ?: "Invalid sort")
}

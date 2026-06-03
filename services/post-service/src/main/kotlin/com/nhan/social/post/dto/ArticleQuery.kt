package com.nhan.social.post.dto

data class ArticleQuery(
    val page: Int = 0,
    val size: Int = 10,
    val filter: String? = null,
    val sort: String? = null,
)

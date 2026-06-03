package com.nhan.social.post.resource

import com.nhan.social.post.dto.ArticleQuery
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam

class ArticleListParams {

    @QueryParam("page")
    @DefaultValue("0")
    var page: Int = 0

    @QueryParam("size")
    @DefaultValue("10")
    var size: Int = 10

    @QueryParam("filter")
    var filter: String? = null

    @QueryParam("sort")
    var sort: String? = null

    fun toQuery() = ArticleQuery(page, size, filter, sort)
}

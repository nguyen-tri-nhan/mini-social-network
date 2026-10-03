package com.nhan.social.ws

// Luật SUBSCRIBE theo ADR 0009: topic cá nhân chỉ cho đúng chủ nhân,
// topic live-comment của bài viết cho mọi user đã đăng nhập, còn lại từ chối.
object TopicAuthorizer {

    private val USER_TOPIC    = Regex("^user_([0-9a-fA-F-]{36})_[a-z]+$")
    private val ARTICLE_TOPIC = Regex("^article_[0-9a-fA-F-]{36}_comment_added$")

    fun canSubscribe(topic: String, userId: String): Boolean {
        USER_TOPIC.matchEntire(topic)?.let { return it.groupValues[1].equals(userId, ignoreCase = true) }
        return ARTICLE_TOPIC.matches(topic)
    }
}

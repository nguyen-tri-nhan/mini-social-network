package com.nhan.social.ws

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TopicAuthorizerTest {

    private val me    = "11111111-1111-1111-1111-111111111111"
    private val other = "22222222-2222-2222-2222-222222222222"
    private val article = "33333333-3333-3333-3333-333333333333"

    @Test
    fun `own user topics are allowed`() {
        assertTrue(TopicAuthorizer.canSubscribe("user_${me}_notification", me))
        assertTrue(TopicAuthorizer.canSubscribe("user_${me}_ready", me))
        assertTrue(TopicAuthorizer.canSubscribe("user_${me}_chat", me))
    }

    @Test
    fun `another user's topics are denied`() {
        assertFalse(TopicAuthorizer.canSubscribe("user_${other}_notification", me))
        assertFalse(TopicAuthorizer.canSubscribe("user_${other}_chat", me))
    }

    @Test
    fun `user id comparison ignores case`() {
        assertTrue(TopicAuthorizer.canSubscribe("user_${me.uppercase()}_chat", me))
    }

    @Test
    fun `article live comments are open to any authenticated user`() {
        assertTrue(TopicAuthorizer.canSubscribe("article_${article}_comment_added", me))
    }

    @Test
    fun `unknown or malformed topics are denied`() {
        assertFalse(TopicAuthorizer.canSubscribe("room_${article}_chat", me))
        assertFalse(TopicAuthorizer.canSubscribe("user__notification", me))
        assertFalse(TopicAuthorizer.canSubscribe("user_${me}_notification_extra", me))
        assertFalse(TopicAuthorizer.canSubscribe("article_${article}_comment_added_x", me))
        assertFalse(TopicAuthorizer.canSubscribe("", me))
    }
}

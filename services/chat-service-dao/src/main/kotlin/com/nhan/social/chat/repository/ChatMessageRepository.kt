package com.nhan.social.chat.repository

import com.nhan.social.chat.entity.ChatMessage
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

// id là UUIDv7 → ORDER BY id = theo thời gian, phân trang keyset chỉ cần id (§12.4).
@ApplicationScoped
class ChatMessageRepository : PanacheRepositoryBase<ChatMessage, UUID> {

    // true khi đây là tin mới; false khi (sender, clientMessageId) đã có — gửi trùng (§14.3).
    fun insertIfAbsent(m: ChatMessage): Boolean =
        getEntityManager()
            .createNativeQuery(
                "INSERT INTO chat.chat_message " +
                    "(id, conversation_id, sender_id, client_message_id, key_version, nonce, ciphertext, created_at) " +
                    "VALUES (:id, :conversationId, :senderId, :clientMessageId, :keyVersion, :nonce, :ciphertext, :createdAt) " +
                    "ON CONFLICT (sender_id, client_message_id) DO NOTHING",
            )
            .setParameter("id", m.id)
            .setParameter("conversationId", m.conversationId)
            .setParameter("senderId", m.senderId)
            .setParameter("clientMessageId", m.clientMessageId)
            .setParameter("keyVersion", m.keyVersion)
            .setParameter("nonce", m.nonce)
            .setParameter("ciphertext", m.ciphertext)
            .setParameter("createdAt", OffsetDateTime.ofInstant(m.createdAt, ZoneOffset.UTC))
            .executeUpdate() == 1

    fun findBySenderAndClientId(senderId: UUID, clientMessageId: UUID): ChatMessage? =
        find("senderId = ?1 and clientMessageId = ?2", senderId, clientMessageId).firstResult()

    fun page(conversationId: UUID, before: UUID?, size: Int): List<ChatMessage> {
        val newestFirst = Sort.by("id").descending()
        val query = if (before == null) find("conversationId = ?1", newestFirst, conversationId)
                    else find("conversationId = ?1 and id < ?2", newestFirst, conversationId, before)
        return query.page(Page.ofSize(size)).list()
    }

    // Tin cuối của từng conversation mà `me` tham gia, mới nhất trước. JOIN (không LEFT)
    // nên conversation chưa có tin bị ẩn (§14.5); LATERAL + LIMIT 1 đi index (conversation_id, id).
    fun latestPerConversation(me: UUID, before: UUID?, size: Int): List<ChatMessage> {
        val sql = "SELECT last.id FROM chat.conversation_participant p " +
            "JOIN LATERAL (SELECT m.id FROM chat.chat_message m WHERE m.conversation_id = p.conversation_id " +
            "ORDER BY m.id DESC LIMIT 1) last ON true " +
            "WHERE p.user_id = :me" + (if (before != null) " AND last.id < :before" else "") +
            " ORDER BY last.id DESC LIMIT :size"
        val q = getEntityManager().createNativeQuery(sql)
            .setParameter("me", me)
            .setParameter("size", size)
        if (before != null) q.setParameter("before", before)
        @Suppress("UNCHECKED_CAST")
        val ids = q.resultList as List<UUID>
        if (ids.isEmpty()) return emptyList()
        val byId = list("id in ?1", ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    fun unreadConversationIds(me: UUID, conversationIds: Collection<UUID>): Set<UUID> {
        if (conversationIds.isEmpty()) return emptySet()
        @Suppress("UNCHECKED_CAST")
        return (getEntityManager()
            .createNativeQuery("SELECT p.conversation_id FROM chat.conversation_participant p " +
                "WHERE p.user_id = :me AND p.conversation_id IN (:ids) AND $UNREAD")
            .setParameter("me", me)
            .setParameter("ids", conversationIds)
            .resultList as List<UUID>).toSet()
    }

    // Số conversation có tin chưa đọc — badge icon chat (§13.3).
    fun countUnreadConversations(me: UUID): Long =
        (getEntityManager()
            .createNativeQuery(
                "SELECT count(*) FROM chat.conversation_participant p WHERE p.user_id = :me AND $UNREAD",
            )
            .setParameter("me", me)
            .singleResult as Number).toLong()

    companion object {
        // Có tin của người khác mới hơn mốc đã đọc — UUIDv7 nên so id thay cho thời gian.
        private const val UNREAD = "EXISTS (SELECT 1 FROM chat.chat_message m " +
            "WHERE m.conversation_id = p.conversation_id AND m.sender_id <> :me " +
            "AND (p.last_read_message_id IS NULL OR m.id > p.last_read_message_id))"
    }
}

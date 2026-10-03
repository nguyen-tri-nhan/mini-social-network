package com.nhan.social.chat.repository

import com.nhan.social.chat.entity.ConversationParticipant
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ParticipantRepository : PanacheRepositoryBase<ConversationParticipant, UUID> {

    fun findByConversation(conversationId: UUID): List<ConversationParticipant> =
        list("conversationId", conversationId)

    fun findByConversations(conversationIds: Collection<UUID>): List<ConversationParticipant> =
        if (conversationIds.isEmpty()) emptyList() else list("conversationId in ?1", conversationIds)

    fun findByUser(userId: UUID): List<ConversationParticipant> = list("userId", userId)

    fun findParticipant(conversationId: UUID, userId: UUID): ConversationParticipant? =
        find("conversationId = ?1 and userId = ?2", conversationId, userId).firstResult()

    // Chỉ tiến lên — nhiều tab gửi lệch thứ tự không kéo lùi được (§13.3).
    fun advanceLastRead(conversationId: UUID, userId: UUID, messageId: UUID): Int =
        update(
            "lastReadMessageId = ?3 where conversationId = ?1 and userId = ?2 " +
                "and (lastReadMessageId is null or lastReadMessageId < ?3)",
            conversationId, userId, messageId,
        )
}

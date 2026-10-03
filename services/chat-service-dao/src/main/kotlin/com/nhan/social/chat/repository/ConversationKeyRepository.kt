package com.nhan.social.chat.repository

import com.nhan.social.chat.entity.ConversationKey
import com.nhan.social.chat.entity.ConversationKeyId
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ConversationKeyRepository : PanacheRepositoryBase<ConversationKey, ConversationKeyId> {
    fun findKey(conversationId: UUID, version: Int): ConversationKey? =
        findById(ConversationKeyId(conversationId, version))

    fun findByConversations(conversationIds: Collection<UUID>): List<ConversationKey> =
        if (conversationIds.isEmpty()) emptyList() else list("conversationId in ?1", conversationIds)
}

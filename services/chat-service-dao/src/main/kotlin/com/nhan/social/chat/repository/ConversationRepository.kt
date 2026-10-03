package com.nhan.social.chat.repository

import com.nhan.social.chat.entity.Conversation
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ConversationRepository : PanacheRepositoryBase<Conversation, UUID> {

    // true khi chính lệnh này tạo được row. Đồng thời 2 request cùng id: Postgres bắt bên
    // sau chờ bên trước commit rồi DO NOTHING — §13.1. Native SQL phải ghi rõ schema.
    fun insertIfAbsent(id: UUID): Boolean =
        getEntityManager()
            .createNativeQuery("INSERT INTO chat.conversation (id, created_at) VALUES (:id, now()) ON CONFLICT (id) DO NOTHING")
            .setParameter("id", id)
            .executeUpdate() == 1
}

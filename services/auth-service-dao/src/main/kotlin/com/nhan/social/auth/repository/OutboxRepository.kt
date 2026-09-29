package com.nhan.social.auth.repository

import com.nhan.social.auth.entity.OutboxEntry
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class OutboxRepository : PanacheRepositoryBase<OutboxEntry, UUID> {

    // Insert rồi xoá ngay trong cùng transaction: Debezium vẫn đọc được INSERT
    // từ WAL (EventRouter tự bỏ qua DELETE), bảng outbox không phình. flush()
    // bắt buộc — persist+delete trước khi flush thì Hibernate có thể huỷ cả hai,
    // không sinh SQL nào → Debezium không thấy event.
    fun emit(entry: OutboxEntry) {
        persist(entry)
        flush()
        delete(entry)
    }
}

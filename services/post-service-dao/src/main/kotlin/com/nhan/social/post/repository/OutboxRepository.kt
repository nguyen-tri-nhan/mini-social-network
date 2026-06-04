package com.nhan.social.post.repository

import com.nhan.social.post.entity.OutboxEntry
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class OutboxRepository : PanacheRepositoryBase<OutboxEntry, UUID>

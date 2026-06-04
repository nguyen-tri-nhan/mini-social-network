package com.nhan.social.auth.repository

import com.nhan.social.auth.entity.OutboxEntry
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class OutboxRepository : PanacheRepositoryBase<OutboxEntry, UUID>

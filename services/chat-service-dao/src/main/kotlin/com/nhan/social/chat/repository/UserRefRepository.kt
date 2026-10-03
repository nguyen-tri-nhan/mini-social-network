package com.nhan.social.chat.repository

import com.nhan.social.chat.entity.UserRef
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class UserRefRepository : PanacheRepositoryBase<UserRef, UUID> {
    fun findByIds(ids: Collection<UUID>): List<UserRef> =
        if (ids.isEmpty()) emptyList() else list("id in ?1", ids)
}

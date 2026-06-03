package com.nhan.social.interaction.repository

import com.nhan.social.interaction.entity.Vote
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class VoteRepository : PanacheRepositoryBase<Vote, UUID> {

    fun findByUserAndTarget(userId: UUID, targetId: UUID, targetType: String): Vote? =
        find("userId = ?1 and targetId = ?2 and targetType = ?3", userId, targetId, targetType)
            .firstResult()
}

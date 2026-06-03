package com.nhan.social.user.repository

import com.nhan.social.user.entity.UserProfile
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class UserProfileRepository : PanacheRepositoryBase<UserProfile, UUID> {

    fun findByUsername(username: String): UserProfile? =
        find("username", username).firstResult()
}

package com.nhan.social.auth.repository

import com.nhan.social.auth.entity.Credentials
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class CredentialsRepository : PanacheRepositoryBase<Credentials, UUID> {

    fun findByIdentifier(identifier: String): Credentials? =
        find("username = ?1 or email = ?1", identifier).firstResult()

    fun existsByUsername(username: String): Boolean = count("username", username) > 0

    fun existsByEmail(email: String): Boolean = count("email", email) > 0
}

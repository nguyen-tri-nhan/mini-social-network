package com.nhan.social.auth.security

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration

@ApplicationScoped
class JwtService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    lateinit var issuer: String

    fun generateToken(userId: String, username: String, roles: Set<String>): String =
        Jwt.issuer(issuer)
            .subject(userId)
            .groups(roles)
            .claim("username", username)
            .expiresIn(Duration.ofDays(7))
            .sign()
}

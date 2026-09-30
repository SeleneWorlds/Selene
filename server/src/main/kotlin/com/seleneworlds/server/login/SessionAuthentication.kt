package com.seleneworlds.server.login

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.config.SystemConfig
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class SessionAuthentication(
    private val serverConfig: ServerConfig,
    systemConfig: SystemConfig,
    private val clientAuthorization: ClientAuthorization
) {

    data class TokenData(val userId: String)

    val issuer = systemConfig.authBrokerIssuer
    private val jwkProvider: JwkProvider = JwkProviderBuilder(
        URI(systemConfig.authBrokerUrl.trimEnd('/') + "/.well-known/jwks.json").toURL()
    )
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    fun parseJoinToken(token: String): Either<Exception, TokenData> =
        parseClientToken(token, clientAuthorization::authenticateJoin, "Invalid or expired join session")

    fun parseGameToken(token: String): Either<Exception, TokenData> =
        parseClientToken(token, clientAuthorization::authenticateGame, "Invalid or expired game session")

    private fun parseClientToken(
        token: String,
        authenticate: (String) -> String?,
        error: String
    ): Either<Exception, TokenData> {
        authenticate(token)?.let { return TokenData(it).right() }
        if (!serverConfig.insecureMode) {
            return IllegalArgumentException(error).left()
        }
        return try {
            TokenData(JWT.decode(token).subject).right()
        } catch (_: Exception) {
            TokenData(token).right()
        }
    }

    fun parseBrokerToken(token: String, audience: String): Either<Exception, TokenData> {
        try {
            val decoded = if (serverConfig.insecureMode)
                JWT.decode(token)
            else {
                val keyId = JWT.decode(token).keyId ?: throw IllegalArgumentException("Missing token key ID")
                val publicKey = jwkProvider.get(keyId).publicKey as? RSAPublicKey
                    ?: throw IllegalArgumentException("Token key is not RSA")
                JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
                    .verify(token)
            }
            return TokenData(decoded.subject).right()
        } catch (e: Exception) {
            if (serverConfig.insecureMode) {
                return TokenData(token).right()
            }
            return e.left()
        }
    }

}

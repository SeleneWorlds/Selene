package com.seleneworlds.server.login

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.config.ServerConfig
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class SessionAuthentication(
    private val config: ServerConfig,
    private val serverHeartbeat: ServerHeartbeat
) {

    data class TokenData(val userId: String)

    val issuer = config.authBrokerIssuer
    private val jwkProvider: JwkProvider = JwkProviderBuilder(URI(config.authBrokerJwksUrl).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    fun parseToken(token: String): Either<Exception, TokenData> {
        try {
            val decoded = if (config.insecureMode)
                JWT.decode(token)
            else {
                val keyId = JWT.decode(token).keyId ?: throw IllegalArgumentException("Missing token key ID")
                val publicKey = jwkProvider.get(keyId).publicKey as? RSAPublicKey
                    ?: throw IllegalArgumentException("Token key is not RSA")
                JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(issuer)
                    .withAudience(serverHeartbeat.serverId)
                    .build()
                    .verify(token)
            }
            return TokenData(decoded.subject).right()
        } catch (e: Exception) {
            if (config.insecureMode) {
                return TokenData(token).right()
            }
            return e.left()
        }
    }

}

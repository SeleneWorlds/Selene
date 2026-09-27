package com.seleneworlds.server.login

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class ClientAuthorization {
    data class BrokerFlow(
        val clientState: String,
        val clientRedirectUri: String,
        val clientCodeChallenge: String,
        val brokerState: String,
        val brokerCodeVerifier: String,
        val expiresAt: Instant
    )

    data class AuthorizationCode(
        val userId: String,
        val redirectUri: String,
        val codeChallenge: String,
        val expiresAt: Instant
    )

    data class ClientSession(val userId: String, val expiresAt: Instant)

    private val brokerFlows = ConcurrentHashMap<String, BrokerFlow>()
    private val authorizationCodes = ConcurrentHashMap<String, AuthorizationCode>()
    private val sessions = ConcurrentHashMap<String, ClientSession>()

    fun begin(clientState: String, redirectUri: String, codeChallenge: String): BrokerFlow {
        prune()
        val flow = BrokerFlow(
            clientState,
            redirectUri,
            codeChallenge,
            randomValue(),
            randomValue(48),
            Instant.now().plusSeconds(600)
        )
        brokerFlows[flow.brokerState] = flow
        return flow
    }

    fun consumeBrokerFlow(state: String): BrokerFlow? = brokerFlows.remove(state)?.takeUnless { it.expiresAt.isBefore(Instant.now()) }

    fun issueCode(flow: BrokerFlow, userId: String): String {
        prune()
        val code = randomValue()
        authorizationCodes[code] = AuthorizationCode(
            userId,
            flow.clientRedirectUri,
            flow.clientCodeChallenge,
            Instant.now().plusSeconds(60)
        )
        return code
    }

    fun exchangeCode(code: String, verifier: String, redirectUri: String): Pair<String, Instant>? {
        val authorization = authorizationCodes.remove(code) ?: return null
        if (authorization.expiresAt.isBefore(Instant.now()) || authorization.redirectUri != redirectUri ||
            !MessageDigest.isEqual(sha256UrlSafe(verifier).toByteArray(), authorization.codeChallenge.toByteArray())) {
            return null
        }
        val token = randomValue(48)
        val expiresAt = Instant.now().plusSeconds(300)
        sessions[token] = ClientSession(authorization.userId, expiresAt)
        return token to expiresAt
    }

    fun authenticate(token: String): String? = sessions[token]?.takeUnless { it.expiresAt.isBefore(Instant.now()) }?.userId

    private fun prune() {
        val now = Instant.now()
        brokerFlows.entries.removeIf { it.value.expiresAt.isBefore(now) }
        authorizationCodes.entries.removeIf { it.value.expiresAt.isBefore(now) }
        sessions.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    companion object {
        private val random = SecureRandom()

        fun sha256UrlSafe(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.US_ASCII))
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

        private fun randomValue(size: Int = 32): String = ByteArray(size).also(random::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    }
}

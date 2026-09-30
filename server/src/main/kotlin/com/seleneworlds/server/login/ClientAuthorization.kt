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

    data class ClientSession(val userId: String, @Volatile var expiresAt: Instant)

    private val brokerFlows = ConcurrentHashMap<String, BrokerFlow>()
    private val authorizationCodes = ConcurrentHashMap<String, AuthorizationCode>()
    private val joinSessions = ConcurrentHashMap<String, ClientSession>()
    private val gameSessions = ConcurrentHashMap<String, ClientSession>()

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
        joinSessions[token] = ClientSession(authorization.userId, expiresAt)
        return token to expiresAt
    }

    fun authenticateJoin(token: String): String? = authenticate(joinSessions, token)

    fun authenticateGame(token: String): String? = authenticate(gameSessions, token)

    fun issueGameSession(joinToken: String): Pair<String, Instant>? {
        val joinSession = joinSessions.remove(joinToken)?.takeUnless { it.expiresAt.isBefore(Instant.now()) } ?: return null
        val token = randomValue(48)
        val expiresAt = Instant.now().plusSeconds(GAME_SESSION_SECONDS)
        gameSessions[token] = ClientSession(joinSession.userId, expiresAt)
        return token to expiresAt
    }

    fun renewGameSession(token: String): Instant? {
        val session = gameSessions[token] ?: return null
        synchronized(session) {
            if (session.expiresAt.isBefore(Instant.now())) {
                gameSessions.remove(token, session)
                return null
            }
            return Instant.now().plusSeconds(GAME_SESSION_SECONDS).also { session.expiresAt = it }
        }
    }

    fun revokeGameSession(token: String) {
        gameSessions.remove(token)
    }

    private fun authenticate(sessions: ConcurrentHashMap<String, ClientSession>, token: String): String? =
        sessions[token]?.takeUnless { it.expiresAt.isBefore(Instant.now()) }?.userId

    private fun prune() {
        val now = Instant.now()
        brokerFlows.entries.removeIf { it.value.expiresAt.isBefore(now) }
        authorizationCodes.entries.removeIf { it.value.expiresAt.isBefore(now) }
        joinSessions.entries.removeIf { it.value.expiresAt.isBefore(now) }
        gameSessions.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    companion object {
        const val GAME_SESSION_SECONDS = 3600L
        private val random = SecureRandom()

        fun sha256UrlSafe(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.US_ASCII))
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

        private fun randomValue(size: Int = 32): String = ByteArray(size).also(random::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    }
}

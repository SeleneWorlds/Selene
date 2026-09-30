package com.seleneworlds.server.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClientAuthorizationTest {
    @Test
    fun `authorization code is PKCE bound and single use`() {
        val authorization = ClientAuthorization()
        val verifier = "client-verifier"
        val flow = authorization.begin("client-state", "selene://auth", ClientAuthorization.sha256UrlSafe(verifier))
        val consumedFlow = authorization.consumeBrokerFlow(flow.brokerState)
        assertNotNull(consumedFlow)
        assertNull(authorization.consumeBrokerFlow(flow.brokerState))

        val wrongCode = authorization.issueCode(consumedFlow, "user-1")
        assertNull(authorization.exchangeCode(wrongCode, "wrong-verifier", "selene://auth"))

        val code = authorization.issueCode(consumedFlow, "user-1")
        val session = authorization.exchangeCode(code, verifier, "selene://auth")
        assertNotNull(session)
        assertEquals("user-1", authorization.authenticateJoin(session.first))
        assertNull(authorization.exchangeCode(code, verifier, "selene://auth"))

        val gameSession = authorization.issueGameSession(session.first)
        assertNotNull(gameSession)
        assertNull(authorization.authenticateJoin(session.first))
        assertEquals("user-1", authorization.authenticateGame(gameSession.first))
        assertNotNull(authorization.renewGameSession(gameSession.first))
        authorization.revokeGameSession(gameSession.first)
        assertNull(authorization.authenticateGame(gameSession.first))
    }
}

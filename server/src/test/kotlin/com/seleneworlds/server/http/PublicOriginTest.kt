package com.seleneworlds.server.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicOriginTest {
    @Test
    fun `secure public origins require HTTPS except on loopback`() {
        assertEquals("https://play.example", validatePublicOrigin("https://PLAY.example:443/", false))
        assertEquals("http://localhost:8080", validatePublicOrigin("http://localhost:8080", false))
        assertFailsWith<InvalidPublicOriginException> {
            validatePublicOrigin("http://play.example", false)
        }
    }

    @Test
    fun `insecure mode permits a public HTTP origin`() {
        assertEquals("http://play.example", validatePublicOrigin("http://play.example:80/", true))
    }

    @Test
    fun `public origin rejects credentials paths queries and fragments`() {
        listOf(
            "https://user@play.example",
            "https://play.example/client",
            "https://play.example?tenant=one",
            "https://play.example#fragment",
            "ftp://play.example"
        ).forEach { origin ->
            assertFailsWith<InvalidPublicOriginException>(origin) {
                validatePublicOrigin(origin, false)
            }
        }
    }

    @Test
    fun `loopback recognition is deliberately narrow`() {
        assertTrue(isLoopbackHost("localhost"))
        assertTrue(isLoopbackHost("127.0.0.1"))
        assertTrue(isLoopbackHost("::1"))
        assertFalse(isLoopbackHost("127.0.0.2"))
        assertFalse(isLoopbackHost("localhost.example"))
    }
}

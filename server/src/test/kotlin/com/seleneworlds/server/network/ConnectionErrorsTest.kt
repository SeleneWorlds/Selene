package com.seleneworlds.server.network

import java.net.SocketException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionErrorsTest {
    @Test
    fun `recognizes direct and wrapped connection resets`() {
        assertTrue(SocketException("Connection reset").isConnectionReset())
        assertTrue(SocketException("Connection reset by peer").isConnectionReset())
        assertTrue(IllegalStateException(SocketException("Connection reset")).isConnectionReset())
    }

    @Test
    fun `preserves reporting for other failures`() {
        assertFalse(SocketException("Permission denied").isConnectionReset())
        assertFalse(SocketException().isConnectionReset())
        assertFalse(IllegalStateException("Connection reset").isConnectionReset())
    }

    @Test
    fun `handles cyclic exception causes`() {
        val first = IllegalStateException()
        val second = IllegalStateException(first)
        first.initCause(second)
        assertFalse(first.isConnectionReset())
    }
}

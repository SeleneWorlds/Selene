package com.seleneworlds.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ServerLifecycleTest {
    @Test
    fun `overlapping shutdown requests wait for the last tick and dispose once on the server thread`() {
        val ticking = CountDownLatch(1)
        val releaseTick = CountDownLatch(1)
        val disposing = CountDownLatch(1)
        val releaseDispose = CountDownLatch(1)
        val disposals = AtomicInteger()
        var serverThread: Thread? = null
        val lifecycle = ServerLifecycle {
            assertSame(serverThread, Thread.currentThread())
            disposals.incrementAndGet()
            disposing.countDown()
            assertTrue(releaseDispose.await(5, TimeUnit.SECONDS))
        }
        val executor = Executors.newFixedThreadPool(3)
        try {
            val server = executor.submit {
                lifecycle.run {
                    serverThread = Thread.currentThread()
                    ticking.countDown()
                    assertTrue(releaseTick.await(5, TimeUnit.SECONDS))
                }
            }
            assertTrue(ticking.await(5, TimeUnit.SECONDS))
            val hook = executor.submit { lifecycle.shutdown() }
            val other = executor.submit { lifecycle.shutdown() }
            assertEquals(0, disposals.get())
            releaseTick.countDown()
            assertTrue(disposing.await(5, TimeUnit.SECONDS))
            assertFalse(hook.isDone)
            assertFalse(other.isDone)
            releaseDispose.countDown()
            server.get(5, TimeUnit.SECONDS)
            hook.get(5, TimeUnit.SECONDS)
            other.get(5, TimeUnit.SECONDS)
            lifecycle.shutdown()
            assertEquals(1, disposals.get())
        } finally {
            releaseTick.countDown()
            releaseDispose.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `server callback can request shutdown without waiting on itself`() {
        var disposed = false
        val lifecycle = ServerLifecycle { disposed = true }
        lifecycle.run {
            lifecycle.shutdown()
            assertFalse(lifecycle.running)
            assertFalse(disposed)
        }
        assertTrue(disposed)
    }

    @Test
    fun `shutdown before startup disposes once and prevents startup`() {
        var disposals = 0
        val lifecycle = ServerLifecycle { disposals++ }
        lifecycle.shutdown()
        lifecycle.run { error("Must not start after shutdown") }
        lifecycle.shutdown()
        assertEquals(1, disposals)
    }

    @Test
    fun `failed server work still disposes`() {
        var disposals = 0
        val lifecycle = ServerLifecycle { disposals++ }
        assertFailsWith<IllegalStateException> {
            lifecycle.run { error("Failed tick") }
        }
        lifecycle.shutdown()
        assertEquals(1, disposals)
    }
}

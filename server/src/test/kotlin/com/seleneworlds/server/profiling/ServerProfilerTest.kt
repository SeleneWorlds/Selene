package com.seleneworlds.server.profiling

import com.seleneworlds.server.config.ServerConfig
import jdk.jfr.consumer.RecordingFile
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.test.*

class ServerProfilerTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `capture preserves outcomes counts failures and writes JFR`() {
        val root = Files.createTempDirectory("selene-profile-test").toFile()
        val profiler = ServerProfiler(ServerConfig(savePath = root.path), logger)
        try {
            val scope = ServerProfiler.Scope("test", "callback", "test-bundle", "script.lua:12", 100)
            assertEquals(42, profiler.measure(scope) { 42 })
            assertTrue((profiler.getSummary()["scopes"] as List<*>).isEmpty())
            val path = profiler.startCapture(60)
            assertFailsWith<IllegalStateException> { profiler.startCapture(60) }
            assertEquals(42, profiler.measure(scope) { 42 })
            profiler.recordCount("living monsters", 64)
            val failure = IllegalArgumentException("expected")
            assertSame(failure, assertFailsWith<IllegalArgumentException> { profiler.measure(scope) { throw failure } })
            assertEquals(path, profiler.stopCapture())
            val rows = profiler.getSummary()["scopes"] as List<*>
            val row = rows.single() as Map<*, *>
            assertEquals(2L, row["count"])
            assertEquals(1L, row["failures"])
            assertEquals("test-bundle", row["bundle"])
            assertTrue((row["totalMs"] as Double) >= (row["maximumMs"] as Double))
            val events = RecordingFile.readAllEvents(root.resolve(path).toPath())
                .filter { it.eventType.name == "selene.ServerWork" }
            assertEquals(2, events.size)
            assertTrue(events.any { it.getBoolean("failed") })
            assertEquals("script.lua:12", events.first().getString("source"))
            val bean = java.lang.management.ManagementFactory.getThreadMXBean()
            if (bean.isCurrentThreadCpuTimeSupported && bean.isThreadCpuTimeEnabled) {
                assertTrue(events.all { it.getLong("cpuNanos") >= 0 })
            }
            val counts = RecordingFile.readAllEvents(root.resolve(path).toPath())
                .filter { it.eventType.name == "selene.WorkCount" }
            assertEquals(1, counts.size)
            assertEquals("living monsters", counts.single().getString("label"))
            assertEquals(64, counts.single().getInt("count"))
        } finally {
            profiler.dispose()
            root.deleteRecursively()
        }
    }

    @Test
    fun `bounded capture stops automatically and validates duration`() {
        val root = Files.createTempDirectory("selene-profile-expiry").toFile()
        val profiler = ServerProfiler(ServerConfig(savePath = root.path), logger)
        try {
            assertFailsWith<IllegalArgumentException> { profiler.startCapture(0) }
            assertFailsWith<IllegalArgumentException> { profiler.startCapture(3601) }
            val path = profiler.startCapture(1)
            profiler.measure(ServerProfiler.Scope("test", "automatic")) {}
            val deadline = System.nanoTime() + 5_000_000_000L
            while (profiler.getSummary()["active"] == true && System.nanoTime() < deadline) Thread.sleep(10)
            assertEquals(false, profiler.getSummary()["active"])
            assertTrue(root.resolve(path).length() > 0)
            assertNull(profiler.stopCapture())
            profiler.startCapture(60)
            assertTrue((profiler.getSummary()["scopes"] as List<*>).isEmpty())
        } finally {
            profiler.dispose()
            root.deleteRecursively()
        }
    }
}

package com.seleneworlds.server.profiling

import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.config.ServerConfig
import jdk.jfr.Configuration
import jdk.jfr.Event
import jdk.jfr.EventType
import jdk.jfr.Label
import jdk.jfr.Name
import jdk.jfr.Recording
import jdk.jfr.StackTrace
import org.slf4j.Logger
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Name("selene.ServerWork")
@Label("Selene Server Work")
@StackTrace(false)
class ServerWorkEvent : Event() {
    @Label("Kind") var kind = ""
    @Label("Name") var label = ""
    @Label("Bundle") var bundle = ""
    @Label("Registration source") var source = ""
    @Label("Interval milliseconds") var intervalMs = 0
    @Label("Failed") var failed = false
}

/** Records execution boundaries without requiring scripts to install wrappers. */
class ServerProfiler(private val config: ServerConfig, private val logger: Logger) : Disposable {
    data class Scope(
        val kind: String, val name: String, val bundle: String = "",
        val source: String = "", val intervalMs: Int = 0
    )
    private data class Stats(var count: Long = 0, var total: Long = 0, var maximum: Long = 0, var failures: Long = 0)
    private class Capture(val recording: Recording, val path: String) {
        val stats = linkedMapOf<Scope, Stats>()
    }
    private val eventType = EventType.getEventType(ServerWorkEvent::class.java)
    private val timer = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Profiling capture timer").apply { isDaemon = true }
    }
    @Volatile private var capture: Capture? = null
    private var lastCapture: Capture? = null
    private var expiry: ScheduledFuture<*>? = null

    val isCapturing: Boolean
        get() = capture != null || eventType.isEnabled

    fun <T> measure(scope: Scope, callback: () -> T): T {
        val current = capture
        val recordEvent = eventType.isEnabled
        if (current == null && !recordEvent) return callback()
        val event = if (recordEvent) ServerWorkEvent().apply {
            kind = scope.kind; label = scope.name; bundle = scope.bundle
            source = scope.source; intervalMs = scope.intervalMs
            begin()
        } else null
        val started = System.nanoTime()
        var failed = false
        try {
            return callback()
        } catch (failure: Throwable) {
            failed = true
            throw failure
        } finally {
            val elapsed = System.nanoTime() - started
            event?.apply { this.failed = failed; end(); commit() }
            if (current != null) synchronized(this) {
                if (capture === current) {
                    // Bound memory even if scripts create unlimited unique scopes.
                    val key = if (scope in current.stats || current.stats.size < 4096) scope else OVERFLOW
                    val stats = current.stats.getOrPut(key) { Stats() }
                    stats.count++; stats.total += elapsed
                    stats.maximum = maxOf(stats.maximum, elapsed)
                    if (failed) stats.failures++
                }
            }
        }
    }

    @Synchronized
    fun startCapture(durationSeconds: Int): String {
        require(durationSeconds in 1..3600) { "Capture duration must be between 1 and 3600 seconds" }
        check(capture == null) { "A profiling capture is already running" }
        val path = "profiles/server-${Instant.now().toEpochMilli()}.jfr"
        val destination = config.resolveSavePath(path)
        destination.parentFile.mkdirs()
        val recording = Recording(Configuration.getConfiguration("profile"))
        try {
            recording.enable("selene.ServerWork")
            recording.destination = destination.toPath()
            recording.start()
            val current = Capture(recording, path)
            capture = current
            lastCapture = current
            expiry = timer.schedule({
                synchronized(this) { if (capture === current) stopCapture() }
            }, durationSeconds.toLong(), TimeUnit.SECONDS)
        } catch (failure: Throwable) {
            capture = null
            recording.close()
            throw failure
        }
        logger.info("Profiling capture started for {} seconds: {}", durationSeconds, destination)
        return path
    }

    @Synchronized
    fun stopCapture(): String? {
        val current = capture ?: return null
        capture = null
        expiry?.cancel(false)
        expiry = null
        try {
            current.recording.stop()
        } finally {
            current.recording.close()
        }
        logger.info("Profiling capture saved to {}", current.path)
        current.stats.entries.sortedByDescending { it.value.total }.take(20).forEach { (scope, stats) ->
            logger.info("[server-profile] {} {} bundle={} source={} count={} total_ms={} max_ms={} failures={}",
                scope.kind, scope.name, scope.bundle, scope.source, stats.count,
                stats.total / 1_000_000.0, stats.maximum / 1_000_000.0, stats.failures)
        }
        return current.path
    }

    @Synchronized
    fun getSummary(): Map<String, Any?> = mapOf(
        "active" to (capture != null), "path" to lastCapture?.path,
        "scopes" to lastCapture?.stats?.entries?.sortedByDescending { it.value.total }?.map { (scope, stats) ->
            mapOf("kind" to scope.kind, "name" to scope.name, "bundle" to scope.bundle,
                "source" to scope.source, "intervalMs" to scope.intervalMs, "count" to stats.count,
                "totalMs" to stats.total / 1_000_000.0,
                "averageMs" to stats.total / 1_000_000.0 / stats.count,
                "maximumMs" to stats.maximum / 1_000_000.0, "failures" to stats.failures)
        }.orEmpty()
    )

    override fun dispose() {
        try { stopCapture() } finally { timer.shutdownNow() }
    }

    companion object {
        private val OVERFLOW = Scope("other", "additional scopes")
    }
}

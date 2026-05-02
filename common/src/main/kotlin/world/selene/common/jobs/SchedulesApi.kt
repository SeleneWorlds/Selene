package world.selene.common.jobs

import org.slf4j.Logger
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.script.ScriptTrace
import world.selene.common.lua.util.CallerInfo
import world.selene.common.lua.util.xpCall
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Schedule functions for timeouts, intervals, and periodic events.
 */
@Suppress("SameReturnValue")
class SchedulesApi(
    private val logger: Logger,
    private val mainThreadDispatcher: MainThreadDispatcher
) : Disposable {
    data class LuaTimeout(
        val timeoutId: Int,
        val name: String,
        val callback: LuaValue,
        val intervalMs: Int,
        val registrationSite: CallerInfo,
        var task: ScheduledFuture<*>? = null
    ) : ScriptTrace {
        override fun scriptTrace(): String {
            return "[timeout \"$name\", ${intervalMs}ms] scheduled at <$registrationSite>"
        }
    }

    data class LuaInterval(
        val intervalId: Int,
        val name: String,
        val callback: LuaValue,
        val intervalMs: Int,
        val registrationSite: CallerInfo,
        var task: ScheduledFuture<*>? = null
    ) : ScriptTrace {
        override fun scriptTrace(): String {
            return "[interval \"$name\", ${intervalMs}ms] scheduled at <$registrationSite>"
        }
    }

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var lastSecond = -1
    private var lastMinute = -1
    private var lastHour = -1
    private val timeouts = mutableMapOf<Int, LuaTimeout>()
    private val intervals = mutableMapOf<Int, LuaInterval>()
    private var nextTimeoutId = 1
    private var nextIntervalId = 1

    init {
        executor.scheduleAtFixedRate(::runPeriodicSignals, 0, 1, TimeUnit.SECONDS)
    }

    private fun runPeriodicSignals() {
        val now = LocalDateTime.now()
        val currentSecond = now.second
        val currentMinute = now.minute
        val currentHour = now.hour

        if (lastSecond != currentSecond) {
            lastSecond = currentSecond
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Second.EVENT.invoker().second()
            }
        }

        if (lastMinute != currentMinute) {
            lastMinute = currentMinute
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Minute.EVENT.invoker().minute()
            }
        }

        if (lastHour != currentHour) {
            lastHour = currentHour
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Hour.EVENT.invoker().hour()
            }
        }
    }

    fun setTimeout(
        intervalMs: Int,
        callback: LuaValue,
        name: String?,
        registrationSite: CallerInfo
    ): Int {
        if (intervalMs < 0) {
            throw IllegalArgumentException("Timeout interval must be non-negative")
        }
        val timeoutId = nextTimeoutId++
        val handler = LuaTimeout(timeoutId, name ?: "#$timeoutId", callback, intervalMs, registrationSite)

        val task = executor.schedule({
            runCallback(callback, handler, "Lua Error in Timeout")
            timeouts.remove(timeoutId)
        }, intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        timeouts[timeoutId] = handler
        return timeoutId
    }

    fun clearTimeout(timeoutId: Int) {
        val handler = timeouts.remove(timeoutId)
        handler?.task?.cancel(false)
    }

    fun setInterval(
        intervalMs: Int,
        callback: LuaValue,
        name: String?,
        immediate: Boolean,
        registrationSite: CallerInfo
    ): Int {
        if (intervalMs <= 0) {
            throw IllegalArgumentException("Interval must be positive")
        }
        val intervalId = nextIntervalId++
        val handler = LuaInterval(intervalId, name ?: "#$intervalId", callback, intervalMs, registrationSite)

        val task = executor.scheduleAtFixedRate({
            runCallback(callback, handler, "Lua Error in Interval")
        }, intervalMs.toLong(), intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        intervals[intervalId] = handler

        if (immediate) {
            val callbackLua = callback.state()
            callbackLua.push(callback)
            callbackLua.xpCall(0, 0, handler)
        }

        return intervalId
    }

    fun clearInterval(intervalId: Int) {
        val handler = intervals.remove(intervalId)
        handler?.task?.cancel(false)
    }

    private fun runCallback(callback: LuaValue, trace: ScriptTrace, errorMessage: String) {
        mainThreadDispatcher.runOnMainThread {
            val callbackLua = callback.state()
            try {
                callbackLua.push(callback)
                callbackLua.xpCall(0, 0, trace)
            } catch (e: LuaException) {
                logger.error(errorMessage, e)
            }
        }
    }

    override fun dispose() {
        timeouts.values.forEach { it.task?.cancel(false) }
        timeouts.clear()

        intervals.values.forEach { it.task?.cancel(false) }
        intervals.clear()

        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

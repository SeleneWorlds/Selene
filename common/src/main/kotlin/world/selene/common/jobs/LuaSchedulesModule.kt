package world.selene.common.jobs

import org.slf4j.Logger
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.Signal
import world.selene.common.lua.util.CallerInfo
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.getFieldBoolean
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Schedule functions for timeouts, intervals, and periodic signals.
 */
@Suppress("SameReturnValue")
class LuaSchedulesModule(
    private val logger: Logger,
    private val mainThreadDispatcher: MainThreadDispatcher
) : LuaModule, Disposable {
    override val name = "selene.schedules"

    data class LuaTimeout(
        val timeoutId: Int,
        val name: String,
        val callback: LuaValue,
        val intervalMs: Int,
        val registrationSite: CallerInfo,
        var task: ScheduledFuture<*>? = null
    ) : LuaTrace {
        override fun luaTrace(): String {
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
    ) : LuaTrace {
        override fun luaTrace(): String {
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

    /**
     * Fired every second.
     */
    val secondSignal: Signal = Signal("Second")

    /**
     * Fired every full minute.
     */
    val minuteSignal: Signal = Signal("Minute")

    /**
     * Fired every full hour.
     */
    val hourSignal: Signal = Signal("Hour")

    init {
        executor.scheduleAtFixedRate(::runPeriodicSignals, 0, 1, TimeUnit.SECONDS)
    }

    override fun register(table: LuaValue) {
        table.set("EverySecond", secondSignal)
        table.set("EveryMinute", minuteSignal)
        table.set("EveryHour", hourSignal)
        table.register("SetTimeout", this::luaSetTimeout)
        table.register("ClearTimeout", this::luaClearTimeout)
        table.register("SetInterval", this::luaSetInterval)
        table.register("ClearInterval", this::luaClearInterval)
    }

    private fun runPeriodicSignals() {
        val now = LocalDateTime.now()
        val currentSecond = now.second
        val currentMinute = now.minute
        val currentHour = now.hour

        if (lastSecond != currentSecond) {
            lastSecond = currentSecond
            mainThreadDispatcher.runOnMainThread {
                secondSignal.emit()
            }
        }

        if (lastMinute != currentMinute) {
            lastMinute = currentMinute
            mainThreadDispatcher.runOnMainThread {
                minuteSignal.emit()
            }
        }

        if (lastHour != currentHour) {
            lastHour = currentHour
            mainThreadDispatcher.runOnMainThread {
                hourSignal.emit()
            }
        }
    }

    /**
     * Schedules a function to run once after a delay.
     *
     * ```signatures
     * SetTimeout(intervalMs: number, callback: function, options: table{name: string}) -> timeoutId: number
     * ```
     */
    private fun luaSetTimeout(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        if (intervalMs < 0) {
            return lua.error(IllegalArgumentException("Timeout interval must be non-negative"))
        }

        val name = lua.getFieldString(3, "name")

        val registrationSite = lua.getCallerInfo()
        val timeoutId = nextTimeoutId++
        val handler = LuaTimeout(timeoutId, name ?: "#$timeoutId", callback, intervalMs, registrationSite)

        val task = executor.schedule({
            mainThreadDispatcher.runOnMainThread {
                val lua = callback.state()
                try {
                    lua.push(callback)
                    lua.xpCall(0, 0, handler)
                } catch (e: LuaException) {
                    logger.error("Lua Error in Timeout", e)
                }
            }
            timeouts.remove(timeoutId)
        }, intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        timeouts[timeoutId] = handler
        lua.push(timeoutId)
        return 1
    }

    /**
     * Cancels a scheduled timeout by its ID.
     *
     * ```signatures
     * ClearTimeout(timeoutId: number)
     * ```
     */
    private fun luaClearTimeout(lua: Lua): Int {
        val timeoutId = lua.checkInt(1)
        val handler = timeouts.remove(timeoutId)
        handler?.task?.cancel(false)
        return 0
    }

    /**
     * Schedules a function to run repeatedly at intervals.
     *
     * ```signatures
     * SetInterval(intervalMs: number, callback: function, options: table{name: string, immediate: boolean}) -> intervalId: number
     * ```
     */
    private fun luaSetInterval(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val name = lua.getFieldString(3, "name")
        val immediate = lua.getFieldBoolean(3, "immediate") ?: false

        if (intervalMs <= 0) {
            return lua.error(IllegalArgumentException("Interval must be positive"))
        }

        val registrationSite = lua.getCallerInfo()
        val intervalId = nextIntervalId++
        val handler = LuaInterval(intervalId, name ?: "#$intervalId", callback, intervalMs, registrationSite)

        val task = executor.scheduleAtFixedRate({
            mainThreadDispatcher.runOnMainThread {
                val lua = callback.state()
                try {
                    lua.push(callback)
                    lua.xpCall(0, 0, handler)
                } catch (e: LuaException) {
                    logger.error("Lua Error in Interval", e)
                }
            }
        }, intervalMs.toLong(), intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        intervals[intervalId] = handler

        if (immediate) {
            lua.push(callback)
            lua.xpCall(0, 0, handler)
        }

        lua.push(intervalId)
        return 1
    }

    /**
     * Cancels a scheduled interval by its ID.
     *
     * ```signatures
     * ClearInterval(intervalId: number)
     * ```
     */
    private fun luaClearInterval(lua: Lua): Int {
        val intervalId = lua.checkInt(1)
        val handler = intervals.remove(intervalId)
        handler?.task?.cancel(false)
        return 0
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

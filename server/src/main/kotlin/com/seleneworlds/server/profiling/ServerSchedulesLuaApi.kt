package com.seleneworlds.server.profiling

import com.seleneworlds.common.jobs.SchedulesApi
import com.seleneworlds.common.jobs.ScheduleEvents
import com.seleneworlds.common.bundles.BundleExecutionContext

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.util.Disposable

/**
 * Schedule functions for timeouts, intervals, and periodic events.
 */
class ServerSchedulesLuaApi(private val api: SchedulesApi, private val profiler: ServerProfiler) : LuaModule, Disposable {
    override val name = "selene.schedules"

    private val secondEvent = LuaEventSink(ScheduleEvents.Second.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        val scope = scope("signal", "everySecond", trace)
        ScheduleEvents.Second {
            profiler.measure(scope) {
                val lua = callback.state()
                lua.push(callback)
                lua.xpCall(0, 0, trace)
            }
        }
    }
    private val minuteEvent = LuaEventSink(ScheduleEvents.Minute.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        val scope = scope("signal", "everyMinute", trace)
        ScheduleEvents.Minute {
            profiler.measure(scope) {
                val lua = callback.state()
                lua.push(callback)
                lua.xpCall(0, 0, trace)
            }
        }
    }
    private val hourEvent = LuaEventSink(ScheduleEvents.Hour.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        val scope = scope("signal", "everyHour", trace)
        ScheduleEvents.Hour {
            profiler.measure(scope) {
                val lua = callback.state()
                lua.push(callback)
                lua.xpCall(0, 0, trace)
            }
        }
    }

    override fun register(table: LuaValue) {
        table.set("everySecond", secondEvent)
        table.set("everyMinute", minuteEvent)
        table.set("everyHour", hourEvent)
        table.register("setTimeout", ::setTimeout)
        table.register("clearTimeout", ::clearTimeout)
        table.register("setInterval", ::setInterval)
        table.register("clearInterval", ::clearInterval)
    }

    private fun setTimeout(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)
        val name = lua.getFieldString(3, "name")
        val trace = lua.getCallerInfo()
        val scope = scope("timeout", name.orEmpty(), trace, intervalMs)
        val timeoutId = api.setTimeout(intervalMs) {
            val lua = callback.state()
            try {
                profiler.measure(scope) {
                    lua.push(callback)
                    lua.xpCall(0, 0, ConstantTrace("[timeout \"$name\", ${intervalMs}ms] scheduled at <$trace>"))
                }
            } catch (e: LuaException) {
                logger.error("Lua error in timeout", e)
            }
        }
        lua.push(timeoutId)
        return 1
    }

    private fun clearTimeout(lua: Lua): Int {
        api.clearTimeout(lua.checkInt(1))
        return 0
    }

    private fun setInterval(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)
        val name = lua.getFieldString(3, "name")
        val trace = lua.getCallerInfo()
        val scope = scope("interval", name.orEmpty(), trace, intervalMs)
        val intervalId = api.setInterval(
            intervalMs = intervalMs,
            immediate = lua.getFieldBoolean(3, "immediate") ?: false
        ) {
            val lua = callback.state()
            try {
                profiler.measure(scope) {
                    lua.push(callback)
                    lua.xpCall(0, 0, ConstantTrace("[interval \"$name\", ${intervalMs}ms] scheduled at <$trace>"))
                }
            } catch (e: LuaException) {
                logger.error("Lua error in interval", e)
            }
        }
        lua.push(intervalId)
        return 1
    }

    private fun clearInterval(lua: Lua): Int {
        api.clearInterval(lua.checkInt(1))
        return 0
    }

    private fun scope(kind: String, name: String, trace: ScriptTrace, intervalMs: Int = 0) =
        ServerProfiler.Scope(kind, name, BundleExecutionContext.currentBundle?.manifest?.name.orEmpty(),
            trace.scriptTrace(), intervalMs)

    override fun dispose() {
        api.dispose()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SchedulesApi::class.java)
    }
}

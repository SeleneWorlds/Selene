package world.selene.common.jobs

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.script.ClosureTrace
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.*
import world.selene.common.script.ScriptTrace
import world.selene.common.util.Disposable

/**
 * Schedule functions for timeouts, intervals, and periodic events.
 */
class SchedulesLuaApi(private val api: SchedulesApi) : LuaModule, Disposable {
    override val name = "selene.schedules"

    private val secondEvent = LuaEventSink(ScheduleEvents.Second.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ScheduleEvents.Second {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }
    private val minuteEvent = LuaEventSink(ScheduleEvents.Minute.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ScheduleEvents.Minute {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }
    private val hourEvent = LuaEventSink(ScheduleEvents.Hour.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ScheduleEvents.Hour {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    override fun register(table: LuaValue) {
        table.set("EverySecond", secondEvent)
        table.set("EveryMinute", minuteEvent)
        table.set("EveryHour", hourEvent)
        table.register("SetTimeout", ::luaSetTimeout)
        table.register("ClearTimeout", ::luaClearTimeout)
        table.register("SetInterval", ::luaSetInterval)
        table.register("ClearInterval", ::luaClearInterval)
    }

    private fun luaSetTimeout(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)
        val name = lua.getFieldString(3, "name")
        val trace = lua.getCallerInfo()
        val timeoutId = api.setTimeout(intervalMs) {
            val lua = callback.state()
            try {
                lua.push(callback)
                lua.xpCall(0, 0, ClosureTrace { "[timeout \"$name\", ${intervalMs}ms] scheduled at <$trace>" })
            } catch (e: LuaException) {
                logger.error("Lua error in timeout", e)
            }
        }
        lua.push(timeoutId)
        return 1
    }

    private fun luaClearTimeout(lua: Lua): Int {
        api.clearTimeout(lua.checkInt(1))
        return 0
    }

    private fun luaSetInterval(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)
        val name = lua.getFieldString(3, "name")
        val trace = lua.getCallerInfo()
        val intervalId = api.setInterval(
            intervalMs = intervalMs,
            immediate = lua.getFieldBoolean(3, "immediate") ?: false
        ) {
            val lua = callback.state()
            try {
                lua.push(callback)
                lua.xpCall(0, 0, ClosureTrace { "[timeout \"$name\", ${intervalMs}ms] scheduled at <$trace>" })
            } catch (e: LuaException) {
                logger.error("Lua error in interval", e)
            }
        }
        lua.push(intervalId)
        return 1
    }

    private fun luaClearInterval(lua: Lua): Int {
        api.clearInterval(lua.checkInt(1))
        return 0
    }

    override fun dispose() {
        api.dispose()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SchedulesApi::class.java)
    }
}

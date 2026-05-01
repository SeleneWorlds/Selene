package world.selene.common.jobs

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.getFieldBoolean
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall
import world.selene.common.util.Disposable

/**
 * Schedule functions for timeouts, intervals, and periodic events.
 */
class SchedulesLuaApi(private val api: SchedulesApi) : LuaModule, Disposable {
    override val name = "selene.schedules"

    private val secondEvent = LuaEventSink(ScheduleEvents.Second.EVENT) { callback: LuaValue, trace: LuaTrace ->
        ScheduleEvents.Second {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }
    private val minuteEvent = LuaEventSink(ScheduleEvents.Minute.EVENT) { callback: LuaValue, trace: LuaTrace ->
        ScheduleEvents.Minute {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }
    private val hourEvent = LuaEventSink(ScheduleEvents.Hour.EVENT) { callback: LuaValue, trace: LuaTrace ->
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

        val timeoutId = api.setTimeout(
            intervalMs = intervalMs,
            callback = callback,
            name = lua.getFieldString(3, "name"),
            registrationSite = lua.getCallerInfo()
        )
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

        val intervalId = api.setInterval(
            intervalMs = intervalMs,
            callback = callback,
            name = lua.getFieldString(3, "name"),
            immediate = lua.getFieldBoolean(3, "immediate") ?: false,
            registrationSite = lua.getCallerInfo()
        )
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
}

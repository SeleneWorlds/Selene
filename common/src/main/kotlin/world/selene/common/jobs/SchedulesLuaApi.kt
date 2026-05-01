package world.selene.common.jobs

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaTrace
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
        table.register("SetTimeout", api::luaSetTimeout)
        table.register("ClearTimeout", api::luaClearTimeout)
        table.register("SetInterval", api::luaSetInterval)
        table.register("ClearInterval", api::luaClearInterval)
    }

    override fun dispose() {
        api.dispose()
    }
}

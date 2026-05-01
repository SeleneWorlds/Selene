package world.selene.common.jobs

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register
import world.selene.common.util.Disposable

/**
 * Schedule functions for timeouts, intervals, and periodic signals.
 */
class SchedulesLuaApi(private val api: SchedulesApi) : LuaModule, Disposable {
    override val name = "selene.schedules"

    override fun register(table: LuaValue) {
        table.set("EverySecond", api.secondSignal)
        table.set("EveryMinute", api.minuteSignal)
        table.set("EveryHour", api.hourSignal)
        table.register("SetTimeout", api::luaSetTimeout)
        table.register("ClearTimeout", api::luaClearTimeout)
        table.register("SetInterval", api::luaSetInterval)
        table.register("ClearInterval", api::luaClearInterval)
    }

    override fun dispose() {
        api.dispose()
    }
}

package world.selene.client.script

import org.slf4j.Logger
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import world.selene.client.entity.EntityApi
import world.selene.common.lua.LuaManager
import world.selene.common.script.ScriptTrace
import world.selene.common.lua.util.xpCall
import world.selene.common.observable.ObservableMap

class ClientEntityLuaScript(
    private val module: String,
    private val luaManager: LuaManager,
    private val logger: Logger
) : ClientEntityScript, ScriptTrace {

    override fun initialize(entity: EntityApi, data: ObservableMap): Boolean {
        return invokeIfPresent("Initialize") { lua ->
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data, Lua.Conversion.NONE)
            lua.xpCall(2, 0, this)
        }
    }

    override fun tick(entity: EntityApi, data: ObservableMap, delta: Float): Boolean {
        return invokeIfPresent("TickEntity") { lua ->
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data, Lua.Conversion.NONE)
            lua.push(delta)
            lua.xpCall(3, 0, this)
        }
    }

    private inline fun invokeIfPresent(functionName: String, body: (Lua) -> Unit): Boolean {
        val lua = luaManager.lua
        lua.getGlobal("require")
        lua.push(module)
        try {
            lua.pCall(1, 1)
            lua.getField(-1, functionName)
            if (!lua.isFunction(-1)) {
                lua.pop(2)
                return true
            }
            body(lua)
            lua.pop(1)
            return true
        } catch (e: LuaException) {
            logger.error("Lua error in client entity script {}", module, e)
            lua.top = 0
            return false
        }
    }

    override fun scriptTrace(): String {
        return "[client entity script \"$module\"]"
    }
}
package world.selene.client.entity.component

import org.koin.mp.KoinPlatform
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import world.selene.client.maps.Entity
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.ObservableMap
import world.selene.common.lua.xpCall

class ClientScriptComponent(val moduleName: String) : EntityComponent, TickableComponent,
    LuaTrace {
    private val luaManager: LuaManager by KoinPlatform.getKoin().inject()
    private var initialized = false
    private var enabled = true
    private val data = ObservableMap()

    override fun update(entity: Entity, delta: Float) {
        if (!enabled) return
        val lua = luaManager.lua
        lua.getGlobal("require")
        lua.push(moduleName)
        try {
            lua.pCall(1, 1)
        } catch (e: LuaException) {
            e.printStackTrace()
            enabled = false
            return
        }
        if (!initialized) {
            lua.getField(-1, "Initialize")
            if (lua.isFunction(-1)) {
                lua.push(entity, Lua.Conversion.NONE)
                lua.push(data, Lua.Conversion.NONE)
                lua.xpCall(2, 0, this)
            } else lua.pop(1)
            initialized = true
        }
        lua.getField(-1, "TickEntity")
        if (lua.isFunction(-1)) {
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data, Lua.Conversion.NONE)
            lua.push(delta)
            lua.xpCall(3, 0, this)
        } else lua.pop(1)
        lua.pop(1)
    }

    override fun luaTrace(): String {
        return "[client script component \"${moduleName}\"]"
    }
}
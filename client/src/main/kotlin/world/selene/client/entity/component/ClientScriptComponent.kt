package world.selene.client.entity.component

import org.koin.mp.KoinPlatform
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.maps.Entity
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.newTable
import world.selene.common.lua.xpCall

class ClientScriptComponent(val moduleName: String) : EntityComponent, TickableComponent,
    LuaTrace {
    private val luaManager: LuaManager by KoinPlatform.getKoin().inject()
    private var module: LuaValue? = null
    private var data: LuaValue? = null

    override fun update(entity: Entity, delta: Float) {
        val lua = luaManager.lua
        val initPending = module == null
        val data = data ?: lua.newTable {}.also {
            data = it
        }
        lua.push(moduleName)
        luaManager.luaRequire(lua)
        if (initPending) {
            lua.getField(-1, "Initialize")
            if (lua.isFunction(-1)) {
                lua.push(entity, Lua.Conversion.NONE)
                lua.push(data)
                lua.xpCall(2, 0, this)
            } else lua.pop(1)
        }
        lua.getField(-1, "TickEntity")
        if (lua.isFunction(-1)) {
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data)
            lua.push(delta)
            lua.xpCall(3, 0, this)
        } else lua.pop(1)
        lua.pop(1)
    }

    override fun luaTrace(): String {
        return "[client script component \"${moduleName}\"]"
    }
}
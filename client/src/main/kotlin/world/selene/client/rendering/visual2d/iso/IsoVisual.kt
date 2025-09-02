package world.selene.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

interface IsoVisual : Visual2D, LuaMetatableProvider {
    val sortLayerOffset: Int
    val surfaceHeight: Float

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = Visual2D.luaMeta.extend(IsoVisual::class) {
            readOnly(IsoVisual::surfaceHeight)
        }
    }
}
package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual2d.DrawableVisual
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class ReloadableVisualApi(val visual: ReloadableVisual) : LuaMetatableProvider, IsoVisualApi {

    fun getDrawable() = (visual.visual as? DrawableVisual)?.drawable?.api

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return ReloadableVisualLuaApi.luaMeta
    }
}

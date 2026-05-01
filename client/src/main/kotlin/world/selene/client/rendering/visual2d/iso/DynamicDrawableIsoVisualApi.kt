package world.selene.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.IsoVisualApi
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class DynamicDrawableIsoVisualApi(val visual: DynamicDrawableIsoVisual) : IsoVisualApi, LuaMetatableProvider {

    fun getDrawable() = visual.drawable.api

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return DynamicDrawableIsoVisualLuaApi.luaMeta
    }
}

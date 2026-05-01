package world.selene.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.IsoVisualApi
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class DrawableIsoVisualApi(val visual: DrawableIsoVisual) : IsoVisualApi, LuaMetatableProvider {

    fun getDrawable() = visual.drawable.api

    fun getDefinition(): VisualDefinition {
        return visual.visualDefinition
    }

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return DrawableIsoVisualLuaApi.luaMeta
    }
}

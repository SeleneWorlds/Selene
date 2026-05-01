package world.selene.client.rendering.visual2d

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class DrawableVisual2DApi(val visual: DrawableVisual2D) : Visual2DApi, LuaMetatableProvider {

    fun getDrawable() = visual.drawable.api

    fun getDefinition(): VisualDefinition {
        return visual.visualDefinition
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return DrawableVisual2DLuaApi.luaMeta
    }
}

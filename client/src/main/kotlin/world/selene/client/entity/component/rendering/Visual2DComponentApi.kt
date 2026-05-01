package world.selene.client.entity.component.rendering

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class Visual2DComponentApi(val component: Visual2DComponent) : LuaMetatableProvider {

    fun getVisual() = component.visual.api

    fun getRed(): Float = component.red

    fun setRed(value: Float) {
        component.red = value
    }

    fun getGreen(): Float = component.green

    fun setGreen(value: Float) {
        component.green = value
    }

    fun getBlue(): Float = component.blue

    fun setBlue(value: Float) {
        component.blue = value
    }

    fun getAlpha(): Float = component.alpha

    fun setAlpha(value: Float) {
        component.alpha = value
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return Visual2DComponentLuaApi.luaMeta
    }
}

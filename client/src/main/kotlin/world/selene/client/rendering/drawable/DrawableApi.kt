package world.selene.client.rendering.drawable

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

open class DrawableApi(val drawable: Drawable) : LuaMetatableProvider {
    override fun luaMetatable(lua: Lua): LuaMetatable {
        return DrawableLuaApi.luaMeta
    }
}

package com.seleneworlds.client.ui.lua

import party.iroiro.luajava.Lua
import com.seleneworlds.client.ui.HudApi
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object HudLuaApi {

    /**
     * Top-level actors produced by the parsed HUD template.
     *
     * ```property
     * Actors: Actor[]
     * ```
     */
    private fun luaGetActors(lua: Lua): Int {
        val self = lua.checkUserdata<HudApi>(1)
        lua.push(self.getActors(), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Named actors indexed by their LML `id`.
     *
     * ```property
     * ActorsByName: table<string, Actor>
     * ```
     */
    private fun luaGetActorsByName(lua: Lua): Int {
        val self = lua.checkUserdata<HudApi>(1)
        lua.push(self.getActorsByName(), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Finds an actor by its LML `id`.
     *
     * ```signatures
     * GetActor(name: string) -> Actor|nil
     * ```
     */
    private fun luaGetActor(lua: Lua): Int {
        val self = lua.checkUserdata<HudApi>(1)
        val actor = self.getActor(lua.checkString(2))
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    val luaMeta = LuaMappedMetatable(HudApi::class) {
        getter(::luaGetActors)
        getter(::luaGetActorsByName)
        callable(::luaGetActor)
    }
}

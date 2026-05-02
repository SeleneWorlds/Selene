package com.seleneworlds.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata

object DrawableIsoVisualLuaApi {

    /**
     * Surface height of the visual, i.e. the offset applied to other visuals on top of it.
     *
     * ```property
     * SurfaceHeight: number
     * ```
     */
    private fun luaGetSurfaceHeight(lua: Lua): Int {
        val self = lua.checkUserdata<DrawableIsoVisualApi>(1)
        lua.push(self.getSurfaceHeight())
        return 1
    }

    /**
     * Drawable rendered by this visual.
     *
     * ```property
     * Drawable: DrawableApi
     * ```
     */
    private fun luaGetDrawable(lua: Lua): Int {
        val self = lua.checkUserdata<DrawableIsoVisualApi>(1)
        lua.push(self.getDrawable(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Registry definition of the visual.
     *
     * ```property
     * Definition: VisualDefinition
     * ```
     */
    private fun luaGetDefinition(lua: Lua): Int {
        val self = lua.checkUserdata<DrawableIsoVisualApi>(1)
        lua.push(self.getDefinition(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = LuaMappedMetatable(DrawableIsoVisualApi::class) {
        getter(::luaGetSurfaceHeight)
        getter(::luaGetDrawable)
        getter(::luaGetDefinition)
    }
}

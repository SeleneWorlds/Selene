package com.seleneworlds.client.rendering.visual2d

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkUserdata

object DrawableVisual2DLuaApi {

    /**
     * Drawable rendered by this visual.
     *
     * ```property
     * Drawable: DrawableApi
     * ```
     */
    private fun getDrawable(lua: Lua): Int {
        val self = lua.checkUserdata<DrawableVisual2DApi>(1)
        lua.push(self.getDrawable(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Registry definition of this visual.
     *
     * ```property
     * Definition: VisualDefinition
     * ```
     */
    private fun getDefinition(lua: Lua): Int {
        val self = lua.checkUserdata<DrawableVisual2DApi>(1)
        lua.push(self.getDefinition(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = Visual2DLuaApi.luaMeta.extend(DrawableVisual2DApi::class) {
        callable(::getDrawable)
        callable(::getDefinition)
    }
}

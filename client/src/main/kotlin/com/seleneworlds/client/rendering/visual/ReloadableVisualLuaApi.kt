package com.seleneworlds.client.rendering.visual

import party.iroiro.luajava.Lua
import com.seleneworlds.client.rendering.visual2d.Visual2DLuaApi
import com.seleneworlds.common.lua.util.checkUserdata

object ReloadableVisualLuaApi {

    /**
     * Surface height of the visual, i.e. the offset applied to other visuals on top of it.
     *
     * ```property
     * SurfaceHeight: number
     * ```
     */
    private fun getSurfaceHeight(lua: Lua): Int {
        val self = lua.checkUserdata<ReloadableVisualApi>(1)
        lua.push(self.getSurfaceHeight())
        return 1
    }

    /**
     * Drawable rendered by this visual, or nil if this visual is not backed by a Drawable.
     *
     * ```property
     * Drawable: DrawableApi
     * ```
     */
    private fun getDrawable(lua: Lua): Int {
        val self = lua.checkUserdata<ReloadableVisualApi>(1)
        self.getDrawable()?.let {
            lua.push(it, Lua.Conversion.NONE)
        } ?: lua.pushNil()
        return 1
    }

    val luaMeta = Visual2DLuaApi.luaMeta.extend(ReloadableVisualApi::class) {
        callable(::getSurfaceHeight)
        callable(::getDrawable)
    }
}

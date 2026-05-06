package com.seleneworlds.client.entity.component.rendering

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata

object Visual2DComponentLuaApi {

    /**
     * Visual rendered by this component.
     *
     * ```property
     * Visual: DrawableVisual2DApi
     * ```
     */
    private fun getVisual(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getVisual(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Red tint applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Red: number
     * ```
     */
    private fun getRed(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getRed())
        return 1
    }

    /**
     * ```property
     * Red: number
     * ```
     */
    private fun setRed(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setRed(lua.checkFloat(2))
        return 0
    }

    /**
     * Green tint applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Green: number
     * ```
     */
    private fun getGreen(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getGreen())
        return 1
    }

    /**
     * ```property
     * Green: number
     * ```
     */
    private fun setGreen(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setGreen(lua.checkFloat(2))
        return 0
    }

    /**
     * Blue tint applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Blue: number
     * ```
     */
    private fun getBlue(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getBlue())
        return 1
    }

    /**
     * ```property
     * Blue: number
     * ```
     */
    private fun setBlue(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setBlue(lua.checkFloat(2))
        return 0
    }

    /**
     * Opacity applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Alpha: number
     * ```
     */
    private fun getAlpha(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getAlpha())
        return 1
    }

    /**
     * ```property
     * Alpha: number
     * ```
     */
    private fun setAlpha(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setAlpha(lua.checkFloat(2))
        return 0
    }

    val luaMeta = LuaMappedMetatable(Visual2DComponentApi::class) {
        callable(::getVisual)
        callable(::getRed)
        callable(::setRed)
        callable(::getGreen)
        callable(::setGreen)
        callable(::getBlue)
        callable(::setBlue)
        callable(::getAlpha)
        callable(::setAlpha)
    }
}

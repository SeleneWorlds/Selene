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
    private fun luaGetVisual(lua: Lua): Int {
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
    private fun luaGetRed(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getRed())
        return 1
    }

    /**
     * ```property
     * Red: number
     * ```
     */
    private fun luaSetRed(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setRed(lua.checkFloat(3))
        return 0
    }

    /**
     * Green tint applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Green: number
     * ```
     */
    private fun luaGetGreen(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getGreen())
        return 1
    }

    /**
     * ```property
     * Green: number
     * ```
     */
    private fun luaSetGreen(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setGreen(lua.checkFloat(3))
        return 0
    }

    /**
     * Blue tint applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Blue: number
     * ```
     */
    private fun luaGetBlue(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getBlue())
        return 1
    }

    /**
     * ```property
     * Blue: number
     * ```
     */
    private fun luaSetBlue(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setBlue(lua.checkFloat(3))
        return 0
    }

    /**
     * Opacity applied to the visual (0.0 - 1.0).
     *
     * ```property
     * Alpha: number
     * ```
     */
    private fun luaGetAlpha(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        lua.push(component.getAlpha())
        return 1
    }

    /**
     * ```property
     * Alpha: number
     * ```
     */
    private fun luaSetAlpha(lua: Lua): Int {
        val component = lua.checkUserdata<Visual2DComponentApi>(1)
        component.setAlpha(lua.checkFloat(3))
        return 0
    }

    val luaMeta = LuaMappedMetatable(Visual2DComponentApi::class) {
        getter(::luaGetVisual)
        getter(::luaGetRed)
        setter(::luaSetRed)
        getter(::luaGetGreen)
        setter(::luaSetGreen)
        getter(::luaGetBlue)
        setter(::luaSetBlue)
        getter(::luaGetAlpha)
        setter(::luaSetAlpha)
    }
}

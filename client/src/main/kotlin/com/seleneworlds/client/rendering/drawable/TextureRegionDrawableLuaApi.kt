package com.seleneworlds.client.rendering.drawable

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkUserdata

object TextureRegionDrawableLuaApi {

    /**
     * Texture region rendered by this drawable.
     *
     * ```property
     * TextureRegion: TextureRegion
     * ```
     */
    private fun luaGetTextureRegion(lua: Lua): Int {
        val self = lua.checkUserdata<TextureRegionDrawableApi>(1)
        lua.push(self.getTextureRegion(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Gets a new drawable without any offset.
     *
     * ```signatures
     * WithoutOffset() -> TextureRegionDrawableApi
     * ```
     */
    private fun luaWithoutOffset(lua: Lua): Int {
        val self = lua.checkUserdata<TextureRegionDrawableApi>(1)
        lua.push(self.withoutOffset(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = DrawableLuaApi.luaMeta.extend(TextureRegionDrawableApi::class) {
        getter(::luaGetTextureRegion)
        callable(::luaWithoutOffset)
    }
}

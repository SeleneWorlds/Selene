package com.seleneworlds.client.rendering.visual

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.rendering.drawable.AnimatedDrawableApi
import com.seleneworlds.client.rendering.drawable.AnimatedDrawableLuaApi
import com.seleneworlds.client.rendering.drawable.DrawableApi
import com.seleneworlds.client.rendering.drawable.DrawableLuaApi
import com.seleneworlds.client.rendering.drawable.TextureRegionDrawableApi
import com.seleneworlds.client.rendering.drawable.TextureRegionDrawableLuaApi
import com.seleneworlds.client.rendering.visual2d.DrawableVisual2DApi
import com.seleneworlds.client.rendering.visual2d.DrawableVisual2DLuaApi
import com.seleneworlds.client.rendering.visual2d.iso.DrawableIsoVisualApi
import com.seleneworlds.client.rendering.visual2d.iso.DrawableIsoVisualLuaApi
import com.seleneworlds.client.rendering.visual2d.iso.DynamicDrawableIsoVisualApi
import com.seleneworlds.client.rendering.visual2d.iso.DynamicDrawableIsoVisualLuaApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkIdentifier
import com.seleneworlds.common.lua.util.throwError
import com.seleneworlds.common.lua.util.register

/**
 * Create visuals from visual definitions.
 */
class VisualsLuaApi(private val api: VisualsApi) : LuaModule {
    override val name = "selene.visuals.internal"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(ReloadableVisualApi::class, ReloadableVisualLuaApi.luaMeta)
        luaManager.defineMetatable(DrawableApi::class, DrawableLuaApi.luaMeta)
        luaManager.defineMetatable(AnimatedDrawableApi::class, AnimatedDrawableLuaApi.luaMeta)
        luaManager.defineMetatable(TextureRegionDrawableApi::class, TextureRegionDrawableLuaApi.luaMeta)
        luaManager.defineMetatable(DrawableVisual2DApi::class, DrawableVisual2DLuaApi.luaMeta)
        luaManager.defineMetatable(DrawableIsoVisualApi::class, DrawableIsoVisualLuaApi.luaMeta)
        luaManager.defineMetatable(DynamicDrawableIsoVisualApi::class, DynamicDrawableIsoVisualLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        try {
            lua.push(api.create(identifier), Lua.Conversion.NONE)
            return 1
        } catch (e: IllegalArgumentException) {
            return lua.throwError(e.message ?: "Unable to create visual")
        }
    }
}

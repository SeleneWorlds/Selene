package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.rendering.drawable.AnimatedDrawableApi
import world.selene.client.rendering.drawable.AnimatedDrawableLuaApi
import world.selene.client.rendering.drawable.DrawableApi
import world.selene.client.rendering.drawable.DrawableLuaApi
import world.selene.client.rendering.drawable.TextureRegionDrawableApi
import world.selene.client.rendering.drawable.TextureRegionDrawableLuaApi
import world.selene.client.rendering.visual2d.DrawableVisual2DApi
import world.selene.client.rendering.visual2d.DrawableVisual2DLuaApi
import world.selene.client.rendering.visual2d.iso.DrawableIsoVisualApi
import world.selene.client.rendering.visual2d.iso.DrawableIsoVisualLuaApi
import world.selene.client.rendering.visual2d.iso.DynamicDrawableIsoVisualApi
import world.selene.client.rendering.visual2d.iso.DynamicDrawableIsoVisualLuaApi
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkIdentifier
import world.selene.common.lua.util.throwError
import world.selene.common.lua.util.register

/**
 * Create visuals from visual definitions.
 */
class VisualsLuaApi(private val api: VisualsApi) : LuaModule {
    override val name = "selene.visuals"

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

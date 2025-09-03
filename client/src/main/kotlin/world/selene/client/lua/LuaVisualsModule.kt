package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register

/**
 * Provides functions for creating visuals from visual definitions.
 */
class LuaVisualsModule(private val visualManager: VisualManager) : LuaModule {
    override val name = "selene.visuals"

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreate)
    }

    /**
     * Creates a visual instance from a visual definition.
     *
     * ```lua
     * Visual Create(string visualName)
     * ```
     */
    private fun luaCreate(lua: Lua): Int {
        val name = lua.checkString(1)
        val visual = visualManager.createVisual(name, VisualCreationContext())
        lua.push(visual, Lua.Conversion.NONE)
        return 1
    }
}
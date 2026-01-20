package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register
import world.selene.common.lua.util.throwError

/**
 * Create visuals from visual definitions.
 */
@Suppress("SameReturnValue")
class LuaVisualsModule(private val visualManager: VisualManager) : LuaModule {
    override val name = "selene.visuals"

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreate)
    }

    /**
     * Creates a visual instance from a visual definition.
     *
     * ```signatures
     * Create(visualName: string) -> Visual
     * ```
     */
    private fun luaCreate(lua: Lua): Int {
        val name = lua.checkString(1)
        val visual = visualManager.createVisual(name, VisualCreationContext())
            ?: lua.throwError("Failed to create visual: $name")
        lua.push(visual, Lua.Conversion.NONE)
        return 1
    }
}
package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkIdentifier
import world.selene.common.lua.util.register
import world.selene.common.lua.util.throwError

/**
 * Create visuals from visual definitions.
 */
@Suppress("SameReturnValue")
class LuaVisualsModule(private val visualRegistry: VisualRegistry, private val visualFactory: VisualFactory) :
    LuaModule {
    override val name = "selene.visuals"

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreate)
    }

    /**
     * Creates a visual instance from a visual definition.
     *
     * ```signatures
     * Create(identifier: Identifier) -> Visual
     * ```
     */
    private fun luaCreate(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        val visualDef = visualRegistry.get(identifier) ?: lua.throwError("Visual not found: $identifier")
        val visual = visualFactory.createVisual(visualDef, VisualCreationContext())
            ?: lua.throwError("Failed to create visual: $identifier")
        lua.push(visual, Lua.Conversion.NONE)
        return 1
    }
}
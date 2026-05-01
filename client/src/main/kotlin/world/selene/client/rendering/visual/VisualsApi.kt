package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkIdentifier
import world.selene.common.lua.util.throwError

/**
 * Create visuals from visual definitions.
 */
@Suppress("SameReturnValue")
class VisualsApi(
    private val visualRegistry: VisualRegistry,
    private val visualFactory: VisualFactory
) {
    fun luaCreate(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        val visualDef = visualRegistry.get(identifier) ?: lua.throwError("Visual not found: $identifier")
        val visual = ReloadableVisual.Instance(visualFactory, visualDef.asReference, VisualCreationContext())
        lua.push(visual.api, Lua.Conversion.NONE)
        return 1
    }
}

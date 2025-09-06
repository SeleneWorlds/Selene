package world.selene.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkUserdata

interface IsoVisual : Visual2D, LuaMetatableProvider {
    val sortLayerOffset: Int
    val surfaceHeight: Float

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Surface height of the visual, i.e. the offset applied to other visuals on top of it.
         *
         * ```property
         * SurfaceHeight: number
         * ```
         */
        private fun luaGetSurfaceHeight(lua: Lua): Int {
            val self = lua.checkUserdata<IsoVisual>(1)
            lua.push(self.surfaceHeight)
            return 1
        }

        val luaMeta = Visual2D.luaMeta.extend(IsoVisual::class) {
            getter(::luaGetSurfaceHeight)
        }
    }
}
package world.selene.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.util.checkUserdata

class DrawableVisual2D(
    private val visualDefinition: VisualDefinition,
    private val drawable: Drawable
) : Visual2D, LuaMetatableProvider {
    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable.update(delta)
        }
    }

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return drawable.getBounds(x, y, outRect)
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable.render(batch, x, y)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "DrawableVisual2D(visual=${visualDefinition.identifier})"
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Drawable rendered by this visual.
         *
         * ```property
         * Drawable: Drawable
         * ```
         */
        private fun luaGetDrawable(lua: Lua): Int {
            val self = lua.checkUserdata<DrawableVisual2D>(1)
            lua.push(self.drawable, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Registry definition of this visual.
         *
         * ```property
         * Definition: VisualDefinition
         * ```
         */
        private fun luaGetDefinition(lua: Lua): Int {
            val self = lua.checkUserdata<DrawableVisual2D>(1)
            lua.push(self.visualDefinition, Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(DrawableVisual2D::class) {
            getter(::luaGetDrawable)
            getter(::luaGetDefinition)
        }
    }
}


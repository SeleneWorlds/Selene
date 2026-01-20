package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.util.checkUserdata

class DrawableIsoVisual(
    private val visualDefinition: VisualDefinition,
    private val drawable: Drawable,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {
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
        return "DrawableIsoVisual(visual=${visualDefinition.identifier})"
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
            val self = lua.checkUserdata<DrawableIsoVisual>(1)
            lua.push(self.drawable, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Registry definition of the visual.
         *
         * ```property
         * Definition: VisualDefinition
         * ```
         */
        private fun luaGetDefinition(lua: Lua): Int {
            val self = lua.checkUserdata<DrawableIsoVisual>(1)
            lua.push(self.visualDefinition, Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = IsoVisual.luaMeta.extend(DrawableIsoVisual::class) {
            getter(::luaGetDrawable)
            getter(::luaGetDefinition)
        }
    }
}


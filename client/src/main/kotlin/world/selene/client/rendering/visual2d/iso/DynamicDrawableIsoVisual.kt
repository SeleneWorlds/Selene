package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.data.AnimatorVisualDefinition
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.checkUserdata

class DynamicDrawableIsoVisual(
    private val visualDef: AnimatorVisualDefinition,
    private val drawableProvider: () -> Drawable?,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {

    private val drawable: Drawable? get() = drawableProvider()

    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable?.update(delta)
        }
    }

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return drawable?.getBounds(x, y, outRect) ?: outRect.set(x, y, 0f, 0f)
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable?.render(batch, x, y)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "DynamicDrawableIsoVisual(visual=${visualDef.name})"
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Drawable rendered by this visual (this frame).
         *
         * ```property
         * Drawable: Drawable|nil
         * ```
         */
        private fun luaGetDrawable(lua: Lua): Int {
            val self = lua.checkUserdata<DynamicDrawableIsoVisual>(1)
            lua.push(self.drawable, Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = IsoVisual.luaMeta.extend(DynamicDrawableIsoVisual::class) {
            getter(::luaGetDrawable)
        }
    }
}


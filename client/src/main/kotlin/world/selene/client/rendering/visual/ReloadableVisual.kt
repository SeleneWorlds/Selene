package world.selene.client.rendering.visual

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual2d.DrawableVisual
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.data.RegistryReference
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.util.checkUserdata

sealed interface ReloadableVisual : IsoVisual, LuaMetatableProvider {
    val visual: Visual?
    override val sortLayerOffset: Int
    override val surfaceHeight: Float

    fun dispose()
    override fun update(delta: Float)
    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    override fun render(batch: Batch, x: Float, y: Float)

    class Instance(
        private val visualFactory: VisualFactory,
        private val reference: RegistryReference<VisualDefinition>,
        private val context: VisualCreationContext
    ) : ReloadableVisual {
        override var visual: Visual? = null

        init {
            reference.subscribe {
                visual = if (it != null) visualFactory.createVisual(it, context) else null
            }
        }

        override fun update(delta: Float) {
            visual?.update(delta)
        }

        override fun render(batch: Batch, x: Float, y: Float) {
            (visual as? Visual2D)?.render(batch, x, y)
        }

        override fun getBounds(
            x: Float,
            y: Float,
            outRect: Rectangle
        ): Rectangle {
            return (visual as? Visual2D)?.getBounds(x, y, outRect) ?: None.getBounds(x, y, outRect)
        }

        override fun dispose() {
            reference.unsubscribeAll()
        }

        override val sortLayerOffset: Int
            get() = (visual as? IsoVisual)?.sortLayerOffset ?: 0

        override val surfaceHeight: Float
            get() = (visual as? IsoVisual)?.surfaceHeight ?: 0f
    }

    object None : ReloadableVisual {
        override val visual: Visual? = null
        override val sortLayerOffset: Int = 0
        override val surfaceHeight: Float = 0f
        override fun getBounds(
            x: Float,
            y: Float,
            outRect: Rectangle
        ): Rectangle {
            outRect.set(x, y, 0f, 0f)
            return outRect
        }

        override fun update(delta: Float) = Unit
        override fun render(batch: Batch, x: Float, y: Float) = Unit
        override fun dispose() = Unit
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Drawable rendered by this visual, or nil if this visual is not backed by a Drawable.
         *
         * ```property
         * Drawable: Drawable
         * ```
         */
        private fun luaGetDrawable(lua: Lua): Int {
            val self = lua.checkUserdata<ReloadableVisual>(1)
            (self.visual as? DrawableVisual)?.let {
                lua.push(it.drawable, Lua.Conversion.NONE)
            } ?: lua.pushNil()
            return 1
        }

        val luaMeta = IsoVisual.luaMeta.extend(ReloadableVisual::class) {
            getter(::luaGetDrawable)
        }
    }
}
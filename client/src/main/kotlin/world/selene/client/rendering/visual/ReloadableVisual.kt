package world.selene.client.rendering.visual

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.data.RegistryReference

sealed interface ReloadableVisual : IsoVisual {
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
}
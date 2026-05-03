package com.seleneworlds.client.rendering.visual

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.rendering.visual2d.Visual2D
import com.seleneworlds.client.rendering.visual2d.iso.IsoVisual
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.threading.Awaitable

sealed interface ReloadableVisual : IsoVisual {
    val visual: Visual?
    override val api: ReloadableVisualApi
    override val sortLayerOffset: Int
    override val surfaceHeight: Float

    override fun initialize(): Awaitable<Void?>
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
        override val api = ReloadableVisualApi(this)
        private var initializeAwaitable: Awaitable<Void?>? = null

        private fun bindReference() {
            reference.subscribe {
                visual = if (it != null) visualFactory.createVisual(it, context) else null
                visual?.takeIf { initializeAwaitable != null }?.initialize()
            }
        }

        override fun initialize(): Awaitable<Void?> {
            initializeAwaitable?.let { return it }

            val future = Awaitable<Void?>()
            initializeAwaitable = future
            bindReference()

            val visual = visual
            if (visual == null) {
                future.resolve(null)
            } else {
                visual.initialize().whenComplete { _, throwable ->
                    if (throwable != null) {
                        future.reject(throwable)
                    } else {
                        future.resolve(null)
                    }
                }
            }

            return future
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
        override val api = ReloadableVisualApi(this)
        override val sortLayerOffset: Int = 0
        override val surfaceHeight: Float = 0f
        override fun initialize(): Awaitable<Void?> = Awaitable.completed(null)
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

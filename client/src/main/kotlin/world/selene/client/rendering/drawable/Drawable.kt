package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle

interface Drawable {
    val api: DrawableApi
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    fun update(delta: Float)
    fun render(batch: Batch, x: Float, y: Float)
    fun render(batch: Batch, x: Float, y: Float, width: Float, height: Float)
    fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    )

    object Empty : Drawable {
        override val api = DrawableApi(this)

        override fun getBounds(
            x: Float,
            y: Float,
            outRect: Rectangle
        ): Rectangle {
            return outRect.set(x, y, 0f, 0f)
        }

        override fun update(delta: Float) = Unit

        override fun render(batch: Batch, x: Float, y: Float) = Unit

        override fun render(
            batch: Batch,
            x: Float,
            y: Float,
            width: Float,
            height: Float
        ) = Unit

        override fun render(
            batch: Batch,
            x: Float,
            y: Float,
            originX: Float,
            originY: Float,
            width: Float,
            height: Float,
            scaleX: Float,
            scaleY: Float,
            rotation: Float
        ) = Unit

    }
}

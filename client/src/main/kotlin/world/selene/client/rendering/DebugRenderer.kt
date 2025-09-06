package world.selene.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle

class DebugRenderer {

    data class DebugRectangle(val rectangle: Rectangle, val color: Color, val lifeTime: Float) {
        var lifeTimeLeft = lifeTime
    }

    lateinit var shapeRenderer: ShapeRenderer

    val rectangles = mutableListOf<DebugRectangle>()

    fun initialize() {
        shapeRenderer = ShapeRenderer()
    }

    @Suppress("unused")
    fun drawRectangle(rectangle: Rectangle, color: Color = Color.RED, lifeTime: Float = 0f) {
        rectangles.add(DebugRectangle(rectangle, color, lifeTime))
    }

    fun render(matrix: Matrix4) {
        shapeRenderer.projectionMatrix = matrix
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        rectangles.forEach {
            shapeRenderer.color = it.color
            shapeRenderer.rect(it.rectangle.x, it.rectangle.y, it.rectangle.width, it.rectangle.height)
            it.lifeTimeLeft -= Gdx.graphics.deltaTime
        }
        shapeRenderer.end()

        rectangles.removeIf { it.lifeTimeLeft <= 0f }
    }

}
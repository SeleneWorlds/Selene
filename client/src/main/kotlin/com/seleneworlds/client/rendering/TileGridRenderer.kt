package com.seleneworlds.client.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.common.grid.Coordinate
import kotlin.math.hypot

/** Drawn between ground tiles and other scene sprites, rather than over the UI. */
class TileGridRenderer(private val environment: Environment) {
    var visible = false
    private val pixel = lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        Texture(pixmap).also { pixmap.dispose() }
    }

    fun coordinates(): List<Coordinate> {
        if (!visible) return emptyList()
        val manager = environment.cameraManager
        val camera = manager.camera
        val z = manager.focusCoordinate.z
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f
        val corners = listOf(
            environment.grid.screenToCoordinate(camera.position.x - halfWidth, camera.position.y - halfHeight, z),
            environment.grid.screenToCoordinate(camera.position.x + halfWidth, camera.position.y - halfHeight, z),
            environment.grid.screenToCoordinate(camera.position.x - halfWidth, camera.position.y + halfHeight, z),
            environment.grid.screenToCoordinate(camera.position.x + halfWidth, camera.position.y + halfHeight, z)
        )
        return buildList {
            for (x in corners.minOf { it.x } - 1..corners.maxOf { it.x } + 1) {
                for (y in corners.minOf { it.y } - 1..corners.maxOf { it.y } + 1) {
                    val coordinate = Coordinate(x, y, z)
                    val screenX = environment.grid.getScreenX(coordinate)
                    val screenY = environment.grid.getScreenY(coordinate)
                    if (screenX + environment.grid.tileStepX >= camera.position.x - halfWidth &&
                        screenX - environment.grid.tileStepX <= camera.position.x + halfWidth &&
                        screenY + environment.grid.tileStepY >= camera.position.y - halfHeight &&
                        screenY - environment.grid.tileStepY <= camera.position.y + halfHeight) add(coordinate)
                }
            }
        }
    }

    fun render(batch: Batch, coordinate: Coordinate) {
        val grid = environment.grid
        val x = grid.getScreenX(coordinate)
        val y = grid.getScreenY(coordinate)
        val previous = batch.color.toFloatBits()
        batch.setColor(1f, 1f, 1f, 0.4f)
        line(batch, x, y - grid.tileStepY, x + grid.tileStepX, y)
        line(batch, x + grid.tileStepX, y, x, y + grid.tileStepY)
        line(batch, x, y + grid.tileStepY, x - grid.tileStepX, y)
        line(batch, x - grid.tileStepX, y, x, y - grid.tileStepY)
        batch.setPackedColor(previous)
    }

    fun dispose() {
        if (pixel.isInitialized()) pixel.value.dispose()
    }

    private fun line(batch: Batch, x: Float, y: Float, endX: Float, endY: Float) {
        val dx = endX - x
        val dy = endY - y
        batch.draw(pixel.value, x, y - 0.5f, 0f, 0.5f, hypot(dx, dy), 1f, 1f, 1f,
            MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees, 0, 0, 1, 1, false, false)
    }
}

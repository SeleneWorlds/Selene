package com.seleneworlds.server.http

import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.maps.layers.*
import com.seleneworlds.server.maps.tree.MapTree
import com.seleneworlds.server.sync.ScopedChunkView
import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

@Serializable
internal data class WorldMapImage(val x: Int, val y: Int, val z: Int, val width: Int, val height: Int, val floors: List<Int>, val image: String)

internal enum class WorldMapTiles { BASE, ALL }

internal fun worldMapTiles(value: String?): WorldMapTiles = when (value) {
    null, "all" -> WorldMapTiles.ALL
    "base" -> WorldMapTiles.BASE
    else -> throw IllegalArgumentException("Invalid tiles mode; expected base or all")
}

internal fun worldMapTile(view: ScopedChunkView, coordinate: Coordinate, tiles: WorldMapTiles): Int =
    if (tiles == WorldMapTiles.BASE) view.getBaseTileAt(coordinate)
    else view.getAdditionalTilesAt(coordinate).lastOrNull() ?: view.getBaseTileAt(coordinate)

internal fun createWorldMapImage(tree: MapTree, z: Int, tiles: WorldMapTiles = WorldMapTiles.ALL): WorldMapImage {
    val windows = mutableListOf<ChunkWindow>()
    fun collect(current: MapTree) {
        current.layers.filter { DefaultViewer.canView(it) }.forEach { layer ->
            when (layer) {
                is DenseMapLayer -> layer.chunks.forEach { (start, chunk) -> windows.add(ChunkWindow(start.x, start.y, start.z, chunk.size, chunk.size)) }
                is SparseMapLayer -> layer.chunks.keys.forEach { start -> windows.add(ChunkWindow(start.x, start.y, start.z, SparseMapLayer.CHUNK_SIZE, SparseMapLayer.CHUNK_SIZE)) }
                is MapTreeLayer -> collect(layer.mapTree)
            }
        }
    }
    collect(tree)
    val floor = windows.filter { it.z == z }
    val x = floor.minOfOrNull { it.x } ?: 0
    val y = floor.minOfOrNull { it.y } ?: 0
    val width = (floor.maxOfOrNull { it.x.toLong() + it.width } ?: 1) - x
    val height = (floor.maxOfOrNull { it.y.toLong() + it.height } ?: 1) - y
    require(width in 1..16384 && height in 1..16384 && width * height <= 16_777_216) { "Map is too large for a full-resolution overview" }
    val image = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
    // Work in chunks so the composed view does not allocate world-sized tile/annotation arrays.
    floor.distinct().forEach { window ->
        val view = ScopedChunkView.create(tree, DefaultViewer, window)
        for (dy in 0 until window.height) for (dx in 0 until window.width) {
            val coordinate = Coordinate(window.x + dx, window.y + dy, z)
            val tile = worldMapTile(view, coordinate, tiles)
            if (tile != 0) {
                val definition = tree.registries.tiles.get(tile)
                val color = 0xff000000.toInt() or (definition?.mapColor ?: "#ffffff").substring(1).toInt(16)
                image.setRGB(coordinate.x - x, coordinate.y - y, color)
            }
        }
    }
    val bytes = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    return WorldMapImage(x, y, z, image.width, image.height, windows.map { it.z }.distinct().sorted(), "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes))
}

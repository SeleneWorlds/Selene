package world.selene.server.management

import world.selene.common.data.NameIdRegistry
import world.selene.common.data.TileRegistry
import world.selene.server.maps.MapTree
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ExportMapImage(private val tileRegistry: TileRegistry, private val nameIdRegistry: NameIdRegistry) {
    fun export(mapTree: MapTree, outputFile: File) {
        val baseLayer = mapTree.baseLayer
        val startX = -500
        val startY = -500
        val width = 1024
        val height = 1024
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val tileId = baseLayer.getTileId(startX + x, startY + y, 0)
                val tileName = nameIdRegistry.getName("tiles", tileId)
                val tile = tileName?.let { tileRegistry.get(it) }
                if (tile != null) {
                    val colorFromHex = Color.decode(tile.color)
                    image.setRGB(x, y, colorFromHex.rgb)
                }
            }
        }
        image.flush()
        ImageIO.write(image, "png", outputFile)
    }

    fun export(tiles: IntArray, outputFile: File) {
        val width = 64
        val height = 64
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = x + y * width
                val tileId = tiles[index]
                val tileName = nameIdRegistry.getName("tiles", tileId)
                val tile = tileName?.let { tileRegistry.get(it) }
                if (tile != null) {
                    val colorFromHex = Color.decode(tile.color)
                    image.setRGB(x, y, colorFromHex.rgb)
                }
            }
        }
        image.flush()
        ImageIO.write(image, "png", outputFile)
    }
}
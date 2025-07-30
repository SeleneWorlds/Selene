package world.selene.server.sync

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashBasedTable
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.util.ChunkWindow
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Viewer
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapLayer
import world.selene.server.maps.MapTreeLayer
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTilesReplacement

class ScopedChunkView(val window: ChunkWindow) {

    val luaProxy = ScopedChunkViewLuaProxy(this)
    val backingLayers = mutableListOf<MapLayer>()
    val padding = 1
    val paddedWidth = window.width + padding * 2
    val paddedHeight = window.height + padding * 2
    val baseTiles = IntArray(paddedWidth * paddedHeight) { 0 }
    val additionalTiles: ArrayListMultimap<Coordinate, Int> = ArrayListMultimap.create()
    val annotations = HashBasedTable.create<Coordinate, String, Map<*, *>>()

    fun addAdditionalTileFirst(coordinate: Coordinate, tileId: Int) {
        if (window.contains(coordinate)) {
            additionalTiles.get(coordinate).addFirst(tileId)
        }
    }

    fun addAdditionalTile(coordinate: Coordinate, tileId: Int) {
        if (window.contains(coordinate)) {
            additionalTiles.put(coordinate, tileId)
        }
    }

    fun apply(layer: MapLayer) {
        backingLayers.add(layer)
        when (layer) {
            is DenseMapLayer -> {
                for (y in 0 until paddedHeight) {
                    for (x in 0 until paddedWidth) {
                        val index = x + y * paddedWidth
                        baseTiles[index] = layer.getTileId(window.x + x - padding, window.y + y - padding, window.z)
                    }
                }
                annotations.putAll(layer.getAnnotations())
            }

            is MapTreeLayer -> {
                for (y in 0 until paddedHeight) {
                    for (x in 0 until paddedWidth) {
                        val index = x + y * paddedWidth
                        val coordinate = Coordinate(window.x + x - padding, window.y + y - padding, window.z)
                        baseTiles[index] = layer.getBaseTile(coordinate)
                        layer.getAdditionalTiles(coordinate).forEach {
                            addAdditionalTile(coordinate, it)
                        }
                    }
                }
                annotations.putAll(layer.getAnnotations())
            }

            is SparseMapLayer -> {
                for (y in 0 until paddedHeight) {
                    for (x in 0 until paddedWidth) {
                        val index = x + y * paddedWidth
                        val coordinate = Coordinate(window.x + x - padding, window.y + y - padding, window.z)
                        layer.getOperations(coordinate.x, coordinate.y, coordinate.z).forEach { operation ->
                            if (operation is SparseTilePlacement) {
                                addAdditionalTile(coordinate, operation.tileId)
                            } else if (operation is SparseTilesReplacement) {
                                baseTiles[index] = operation.tileId
                                additionalTiles.get(coordinate).clear()
                            } else if (operation is SparseTileRemoval) {
                                if (baseTiles[index] == operation.tileId) {
                                    baseTiles[index] = if (additionalTiles.size() > 0) additionalTiles.get(coordinate)
                                        .removeAt(0) else 0
                                } else {
                                    val additionalTilesInCell = additionalTiles.get(coordinate)
                                    val index = additionalTilesInCell.indexOfFirst { it -> it == operation.tileId }
                                    if (index != -1) {
                                        additionalTilesInCell.removeAt(index)
                                    }
                                }
                            }
                        }
                    }
                }
                annotations.putAll(layer.getAnnotations())
            }
        }
    }

    fun getBaseTileAtRelative(ox: Int, oy: Int): Int {
        return baseTiles[(ox + padding) + (oy + padding) * paddedWidth]
    }

    fun getBaseTileAt(coordinate: Coordinate): Int {
        val x = coordinate.x - window.x + padding
        val y = coordinate.y - window.y + padding
        val index = x + y * paddedWidth
        return baseTiles[index]
    }

    fun getAdditionalTilesAt(coordinate: Coordinate): List<Int> {
        return additionalTiles.get(coordinate)
    }

    companion object {
        fun create(dimension: Dimension, viewer: Viewer, window: ChunkWindow): ScopedChunkView {
            val result = ScopedChunkView(window)
            dimension.mapTree.layers.forEach { layer ->
                if (viewer.canView(layer)) {
                    result.apply(layer)
                }
            }
            return result
        }
    }

    class ScopedChunkViewLuaProxy(val delegate: ScopedChunkView) {
        fun GetAnnotation(lua: Lua): Int {
            val coordinate = lua.checkJavaObject<Coordinate>(2)
            val key = lua.checkString(3)
            val value = delegate.annotations.get(coordinate, key)
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }
    }
}
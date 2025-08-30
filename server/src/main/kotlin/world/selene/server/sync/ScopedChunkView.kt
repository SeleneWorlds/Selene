package world.selene.server.sync

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashBasedTable
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkString
import world.selene.common.util.ChunkWindow
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Viewer
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapLayer
import world.selene.server.maps.MapTreeLayer
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTileAnnotation
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTileSwap
import world.selene.server.maps.SparseTilesReplacement

class ScopedChunkView(val window: ChunkWindow) : LuaMetatableProvider {

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
                        baseTiles[index] =
                            layer.getTileId(Coordinate(window.x + x - padding, window.y + y - padding, window.z))
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
                        layer.getOperations(coordinate).forEach { operation ->
                            if (operation is SparseTilePlacement) {
                                addAdditionalTile(coordinate, operation.tileDef.id)
                            } else if (operation is SparseTilesReplacement) {
                                baseTiles[index] = operation.tileDef.id
                                additionalTiles.get(coordinate).clear()
                            } else if (operation is SparseTileSwap) {
                                if (baseTiles[index] == operation.oldTileDef.id) {
                                    baseTiles[index] = operation.newTileDef.id
                                } else {
                                    val additionalTilesInCell = additionalTiles.get(coordinate)
                                    val index = additionalTilesInCell.indexOfFirst { it == operation.oldTileDef.id }
                                    if (index != -1) {
                                        additionalTilesInCell[index] = operation.newTileDef.id
                                    }
                                }
                            } else if (operation is SparseTileAnnotation) {
                                if (operation.data != null) {
                                    annotations.put(coordinate, operation.key, operation.data)
                                } else {
                                    annotations.remove(coordinate, operation.key)
                                }
                            } else if (operation is SparseTileRemoval) {
                                if (baseTiles[index] == operation.tileDef.id) {
                                    baseTiles[index] = if (additionalTiles.size() > 0) additionalTiles.get(coordinate)
                                        .removeAt(0) else 0
                                } else {
                                    val additionalTilesInCell = additionalTiles.get(coordinate)
                                    val index = additionalTilesInCell.indexOfFirst { it == operation.tileDef.id }
                                    if (index != -1) {
                                        additionalTilesInCell.removeAt(index)
                                    }
                                }
                            }
                        }
                    }
                }
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

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String): Map<*, *>? {
        return annotations.get(coordinate, key)
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

        val luaMeta = LuaMappedMetatable(ScopedChunkView::class) {
            callable("GetAnnotation") {
                val chunkView = it.checkSelf()
                val coordinate = it.checkUserdata<Coordinate>(2)
                val key = it.checkString(3)
                val value = chunkView.annotations.get(coordinate, key)
                it.push(value, Lua.Conversion.FULL)
                1
            }
        }
    }

}
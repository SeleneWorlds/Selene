package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate
import world.selene.server.data.Registries

class MapTree(private val registries: Registries) : LuaMetatableProvider {
    private val listeners = mutableSetOf<MapTreeListener>()
    val layers = mutableListOf<MapLayer>()
    var baseLayer: BaseMapLayer = EmptyMapLayer
    var sparseLayer: MapLayer? = null
    val defaultLayer = object : MapLayer {
        override val name: String = "default"
        override val visibilityTags = mutableSetOf<String>()
        override val collisionTags = mutableSetOf<String>()

        override fun placeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
            if (baseLayer.placeTile(x, y, z, tileId)) {
                return true
            }
            return sparseLayer?.placeTile(x, y, z, tileId) ?: false
        }

        override fun replaceTiles(
            x: Int,
            y: Int,
            z: Int,
            tileId: Int
        ): Boolean {
            if (baseLayer.replaceTiles(x, y, z, tileId)) {
                return true
            }
            return sparseLayer?.replaceTiles(x, y, z, tileId) ?: false
        }

        override fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
            if (baseLayer.removeTile(x, y, z, tileId)) {
                return true
            }
            return sparseLayer?.removeTile(x, y, z, tileId) ?: false
        }

        override fun resetTile(x: Int, y: Int, z: Int) {
            baseLayer.resetTile(x, y, z)
            sparseLayer?.resetTile(x, y, z)
        }

        override fun annotateTile(
            x: Int,
            y: Int,
            z: Int,
            key: String,
            data: Map<*, *>
        ) {
            baseLayer.annotateTile(x, y, z, key, data)
        }

        override fun addVisibilityTag(tagName: String) {
            visibilityTags.add(tagName)
        }

        override fun removeVisibilityTag(tagName: String) {
            visibilityTags.remove(tagName)
        }

        override fun addCollisionTag(tagName: String) {
            collisionTags.add(tagName)
        }

        override fun removeCollisionTag(tagName: String) {
            collisionTags.remove(tagName)
        }
    }

    fun addLayer(layer: MapLayer) {
        if (baseLayer is EmptyMapLayer && layer is BaseMapLayer) {
            baseLayer = layer
        }
        layers.add(layer)
    }

    fun ensureBaseAndSparseLayers() {
        if (baseLayer is EmptyMapLayer) {
            baseLayer = DenseMapLayer("default").also {
                addLayer(it)
            }
        }
        if (sparseLayer == null) {
            sparseLayer = SparseMapLayer("default").also {
                addLayer(it)
            }
        }
    }

    fun getLayer(layerName: String): MapLayer? {
        if (layerName == "default") {
            ensureBaseAndSparseLayers()
            return defaultLayer
        }

        val found = layers.firstOrNull { it.name == layerName }
        if (found != null) {
            return found
        }

        val newLayer = SparseMapLayer(layerName)
        layers.add(newLayer)
        return newLayer
    }

    fun placeTile(x: Int, y: Int, z: Int, tileId: Int, layerName: String = "default"): Boolean {
        val layer = getLayer(layerName)
        val result = layer?.placeTile(x, y, z, tileId) == true
        if (result) {
            notifyListeners(Coordinate(x, y, z))
        }
        return result
    }

    fun replaceTiles(x: Int, y: Int, z: Int, tileId: Int, layerName: String = "default"): Boolean {
        val layer = getLayer(layerName)
        val result = layer?.replaceTiles(x, y, z, tileId) == true
        if (result) {
            notifyListeners(Coordinate(x, y, z))
        }
        return result
    }

    fun removeTile(x: Int, y: Int, z: Int, tileId: Int, layerName: String = "default"): Boolean {
        val layer = getLayer(layerName)
        val result = layer?.removeTile(x, y, z, tileId) == true
        if (result) {
            notifyListeners(Coordinate(x, y, z))
        }
        return result
    }

    fun resetTile(x: Int, y: Int, z: Int, layerName: String) {
        val layer = getLayer(layerName)
        layer?.resetTile(x, y, z)
        notifyListeners(Coordinate(x, y, z))
    }

    fun annotateTile(x: Int, y: Int, z: Int, key: String, data: Map<*, *>, layerName: String = "default") {
        val layer = getLayer(layerName)
        layer?.annotateTile(x, y, z, key, data)
    }

    fun addListener(listener: MapTreeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MapTreeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(coordinate: Coordinate) {
        listeners.forEach { listener ->
            listener.onTileUpdated(coordinate)
        }
    }

    fun applyOperation(operation: SparseOperation, layerName: String = "default") {
        when (operation) {
            is SparseTilePlacement -> {
                placeTile(
                    operation.x,
                    operation.y,
                    operation.z,
                    operation.tileId,
                    layerName
                )
            }

            is SparseTilesReplacement -> {
                replaceTiles(
                    operation.x,
                    operation.y,
                    operation.z,
                    operation.tileId,
                    layerName
                )
            }

            is SparseTileRemoval -> {
                removeTile(
                    operation.x,
                    operation.y,
                    operation.z,
                    operation.tileId,
                    layerName
                )
            }
        }
    }

    fun merge(other: MapTree) {
        other.layers.forEach { otherLayer ->
            when (otherLayer) {
                is DenseMapLayer -> {
                    otherLayer.chunks.forEach { chunkCoordinate, chunk ->
                        chunk.tiles.withIndex().forEach { (index, tileId) ->
                            val relativeX = index % chunk.size
                            val relativeY = index / chunk.size
                            val absoluteX = chunkCoordinate.x + relativeX
                            val absoluteY = chunkCoordinate.y + relativeY
                            placeTile(absoluteX, absoluteY, chunkCoordinate.z, tileId)
                        }
                        chunk.annotations.cellSet().forEach {
                            annotateTile(it.rowKey.x, it.rowKey.y, chunkCoordinate.z, it.columnKey, it.value)
                        }
                    }
                }

                is SparseMapLayer -> {
                    otherLayer.chunks.forEach { chunkCoordinate, chunk ->
                        chunk.operations.forEach { (_, operations) ->
                            operations.forEach { operation ->
                                applyOperation(operation)
                            }
                        }
                        chunk.annotations.cellSet().forEach {
                            annotateTile(it.rowKey.x, it.rowKey.y, chunkCoordinate.z, it.columnKey, it.value)
                        }
                    }
                }
            }
        }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(MapTree::class) {
            callable("Merge") {
                val mapTree = it.checkSelf()
                val other = it.checkJavaObject<MapTree>(2)
                mapTree.merge(other)
                return@callable 0
            }

            callable("PlaceTile") { lua ->
                val mapTree = lua.checkSelf()
                val (coordinate, index) = lua.checkCoordinate(2)
                val tileName = lua.checkString(index + 1)
                val layerName = lua.toString(index + 2)
                val tile = mapTree.registries.tiles.get(tileName)
                if (tile != null) {
                    val tileId = mapTree.registries.mappings.getId("tiles", tileName)
                    if (tileId != null) {
                        mapTree.placeTile(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                        return@callable 0
                    } else {
                        return@callable lua.error(IllegalStateException("Tile $tileName has no id"))
                    }
                } else {
                    return@callable lua.error(IllegalArgumentException("Unknown tile: $tileName"))
                }
            }

            callable("ReplaceTiles") {
                val mapTree = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val tileName = it.checkString(index + 1)
                val layerName = it.toString(index + 2)
                val tile = mapTree.registries.tiles.get(tileName)
                if (tile != null) {
                    val tileId = mapTree.registries.mappings.getId("tiles", tileName)
                    if (tileId != null) {
                        mapTree.replaceTiles(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                        return@callable 0
                    } else {
                        throw IllegalStateException("Tile $tileName has no id")
                    }
                } else {
                    return@callable it.error(IllegalArgumentException("Unknown tile: $tileName"))
                }
            }

            callable("SwapTile") {
                val mapTree = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val sourceTileName = it.checkString(index + 1)
                val targetTileName = it.checkString(index + 2)
                val layerName = it.toString(index + 3)
                val sourceTile = mapTree.registries.tiles.get(sourceTileName)
                if (sourceTile == null) {
                    return@callable it.error(IllegalArgumentException("Unknown tile: $sourceTileName"))
                }
                val sourceTileId = mapTree.registries.mappings.getId("tiles", sourceTileName)
                if (sourceTileId == null) {
                    throw IllegalStateException("Tile $sourceTileName has no id")
                }
                val targetTile = mapTree.registries.tiles.get(targetTileName)
                if (targetTile == null) {
                    return@callable it.error(IllegalArgumentException("Unknown tile: $targetTileName"))
                }
                val targetTileId = mapTree.registries.mappings.getId("tiles", targetTileName)
                if (targetTileId == null) {
                    throw IllegalStateException("Tile $targetTileName has no id")
                }
                mapTree.removeTile(coordinate.x, coordinate.y, coordinate.z, sourceTileId, layerName ?: "default")
                mapTree.placeTile(coordinate.x, coordinate.y, coordinate.z, targetTileId, layerName ?: "default")
                return@callable 0
            }

            callable("RemoveTile") {
                val mapTree = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val tileName = it.checkString(index + 1)
                val layerName = it.toString(index + 2)
                val tile = mapTree.registries.tiles.get(tileName)
                if (tile != null) {
                    val tileId = mapTree.registries.mappings.getId("tiles", tileName)
                    if (tileId != null) {
                        mapTree.removeTile(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                        return@callable 0
                    } else {
                        throw IllegalStateException("Tile $tileName has no id")
                    }
                } else {
                    return@callable it.error(IllegalArgumentException("Unknown tile: $tileName"))
                }
            }

            callable("ResetTile") {
                val mapTree = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val layerName = it.toString(index + 1)
                mapTree.resetTile(coordinate.x, coordinate.y, coordinate.z, layerName ?: "default")
                return@callable 0
            }

            callable("AnnotateTile") {
                val mapTree = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val key = it.checkString(index + 1)
                val table = it.toMap(index + 2) ?: emptyMap()
                val layerName = it.toString(index + 3)
                mapTree.annotateTile(coordinate.x, coordinate.y, coordinate.z, key, table, layerName ?: "default")
                return@callable 0
            }

            callable("SetVisibility") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val enabled = it.checkBoolean(3)
                val tagName = if (it.isString(4)) it.checkString(4) else "default"
                if (enabled) {
                    layer.addVisibilityTag(tagName)
                } else {
                    layer.removeVisibilityTag(tagName)
                }
                return@callable 0
            }

            callable("IsVisible") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                it.push(layer.visibilityTags.contains(tagName))
                return@callable 1
            }

            callable("IsInvisible") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                it.push(!layer.visibilityTags.contains(tagName))
                return@callable 1
            }

            callable("MakeVisible") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                layer.addVisibilityTag(tagName)
                return@callable 0
            }

            callable("MakeInvisible") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                layer.removeVisibilityTag(tagName)
                return@callable 0
            }

            callable("HasCollisions") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                it.push(layer.collisionTags.contains(tagName))
                return@callable 0
            }

            callable("SetCollisions") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val enabled = it.checkBoolean(3)
                val tagName = if (it.isString(4)) it.checkString(4) else "default"
                if (enabled) {
                    layer.addCollisionTag(tagName)
                } else {
                    layer.removeCollisionTag(tagName)
                }
                return@callable 0
            }

            callable("EnableCollisions") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                layer.addCollisionTag(tagName)
                return@callable 0
            }

            callable("DisableCollisions") {
                val mapTree = it.checkSelf()
                val layerName = it.checkString(2)
                val layer = mapTree.getLayer(layerName)
                    ?: return@callable it.error(IllegalArgumentException("Layer $layerName does not exist"))
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                layer.removeCollisionTag(tagName)
                return@callable 0
            }
        }
    }

}
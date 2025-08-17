package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate
import world.selene.server.data.Registries

class MapTree(private val registries: Registries) {
    private val listeners = mutableSetOf<MapTreeListener>()
    val luaProxy = MapTreeLuaProxy(this)
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

    class MapTreeLuaProxy(val delegate: MapTree) {
        fun Merge(lua: Lua): Int {
            val mapTree = lua.checkJavaObject<MapTreeLuaProxy>(-1)
            delegate.merge(mapTree.delegate)
            return 0
        }

        fun PlaceTile(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileName = lua.checkString(index + 1)
            val layerName = lua.toString(index + 2)
            val tile = delegate.registries.tiles.get(tileName)
            if (tile != null) {
                val tileId = delegate.registries.mappings.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.placeTile(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun ReplaceTiles(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileName = lua.checkString(index + 1)
            val layerName = lua.toString(index + 2)
            val tile = delegate.registries.tiles.get(tileName)
            if (tile != null) {
                val tileId = delegate.registries.mappings.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.replaceTiles(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun SwapTile(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val sourceTileName = lua.checkString(index + 1)
            val targetTileName = lua.checkString(index + 2)
            val layerName = lua.toString(index + 3)
            val sourceTile = delegate.registries.tiles.get(sourceTileName)
            if (sourceTile == null) {
                return lua.error(IllegalArgumentException("Unknown tile: $sourceTileName"))
            }
            val sourceTileId = delegate.registries.mappings.getId("tiles", sourceTileName)
            if (sourceTileId == null) {
                throw IllegalStateException("Tile $sourceTileName has no id")
            }
            val targetTile = delegate.registries.tiles.get(targetTileName)
            if (targetTile == null) {
                return lua.error(IllegalArgumentException("Unknown tile: $targetTileName"))
            }
            val targetTileId = delegate.registries.mappings.getId("tiles", targetTileName)
            if (targetTileId == null) {
                throw IllegalStateException("Tile $targetTileName has no id")
            }
            delegate.removeTile(coordinate.x, coordinate.y, coordinate.z, sourceTileId, layerName ?: "default")
            delegate.placeTile(coordinate.x, coordinate.y, coordinate.z, targetTileId, layerName ?: "default")
            return 0
        }

        fun RemoveTile(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileName = lua.checkString(index + 1)
            val layerName = lua.toString(index + 2)
            val tile = delegate.registries.tiles.get(tileName)
            if (tile != null) {
                val tileId = delegate.registries.mappings.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.removeTile(coordinate.x, coordinate.y, coordinate.z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun ResetTile(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val layerName = lua.toString(index + 1)
            delegate.resetTile(coordinate.x, coordinate.y, coordinate.z, layerName ?: "default")
            return 0
        }

        fun AnnotateTile(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val key = lua.checkString(index + 1)
            val table = lua.toMap(index + 2) ?: emptyMap()
            val layerName = lua.toString(index + 3)
            delegate.annotateTile(coordinate.x, coordinate.y, coordinate.z, key, table, layerName ?: "default")
            return 0
        }

        fun SetVisibility(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val enabled = lua.checkBoolean(3)
            val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
            if (enabled) {
                layer.addVisibilityTag(tagName)
            } else {
                layer.removeVisibilityTag(tagName)
            }
            return 0
        }

        fun IsVisible(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(layer.visibilityTags.contains(tagName))
            return 1
        }

        fun IsInvisible(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(!layer.visibilityTags.contains(tagName))
            return 1
        }

        fun MakeVisible(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.addVisibilityTag(tagName)
            return 0
        }

        fun MakeInvisible(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.removeVisibilityTag(tagName)
            return 0
        }

        fun HasCollisions(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(layer.collisionTags.contains(tagName))
            return 0
        }

        fun SetCollisions(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val enabled = lua.checkBoolean(3)
            val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
            if (enabled) {
                layer.addCollisionTag(tagName)
            } else {
                layer.removeCollisionTag(tagName)
            }
            return 0
        }

        fun EnableCollisions(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.addCollisionTag(tagName)
            return 0
        }

        fun DisableCollisions(lua: Lua): Int {
            val layerName = lua.checkString(2)
            val layer = delegate.getLayer(layerName)
                ?: return lua.error(IllegalArgumentException("Layer $layerName does not exist"))
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.removeCollisionTag(tagName)
            return 0
        }
    }

}
package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.NameIdRegistry
import world.selene.common.data.TileRegistry
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString

class MapTree(private val tileRegistry: TileRegistry, private val nameIdRegistry: NameIdRegistry) {
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
        return layer?.placeTile(x, y, z, tileId) == true
    }

    fun replaceTiles(x: Int, y: Int, z: Int, tileId: Int, layerName: String = "default"): Boolean {
        val layer = getLayer(layerName)
        return layer?.replaceTiles(x, y, z, tileId) == true
    }

    fun removeTile(x: Int, y: Int, z: Int, tileId: Int, layerName: String = "default"): Boolean {
        val layer = getLayer(layerName)
        return layer?.removeTile(x, y, z, tileId) == true
    }

    fun resetTile(x: Int, y: Int, z: Int, layerName: String) {
        val layer = getLayer(layerName)
        layer?.resetTile(x, y, z)
    }

    fun annotateTile(x: Int, y: Int, z: Int, key: String, data: Map<*, *>, layerName: String = "default") {
        val layer = getLayer(layerName)
        layer?.annotateTile(x, y, z, key, data)
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
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            val tileName = lua.checkString(5)
            val layerName = lua.toString(6)
            val tile = delegate.tileRegistry.get(tileName)
            if (tile != null) {
                val tileId = delegate.nameIdRegistry.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.placeTile(x, y, z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun ReplaceTiles(lua: Lua): Int {
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            val tileName = lua.checkString(5)
            val layerName = lua.toString(6)
            val tile = delegate.tileRegistry.get(tileName)
            if (tile != null) {
                val tileId = delegate.nameIdRegistry.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.replaceTiles(x, y, z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun RemoveTile(lua: Lua): Int {
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            val tileName = lua.checkString(5)
            val layerName = lua.toString(6)
            val tile = delegate.tileRegistry.get(tileName)
            if (tile != null) {
                val tileId = delegate.nameIdRegistry.getId("tiles", tileName)
                if (tileId != null) {
                    delegate.removeTile(x, y, z, tileId, layerName ?: "default")
                    return 0
                } else {
                    throw IllegalStateException("Tile $tileName has no id")
                }
            } else {
                return lua.error(IllegalArgumentException("Unknown tile: $tileName"))
            }
        }

        fun ResetTile(lua: Lua): Int {
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            val layerName = lua.toString(5)
            delegate.resetTile(x, y, z, layerName ?: "default")
            return 0
        }

        fun AnnotateTile(lua: Lua): Int {
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            val key = lua.checkString(5)
            val table = lua.toMap(6) ?: emptyMap()
            val layerName = lua.toString(7)
            delegate.annotateTile(x, y, z, key, table, layerName ?: "default")
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
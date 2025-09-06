package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.data.TileDefinition
import world.selene.common.lua.*
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

        override fun placeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
            if (baseLayer.placeTile(coordinate, tileDef)) {
                return true
            }
            return sparseLayer?.placeTile(coordinate, tileDef) ?: false
        }

        override fun swapTile(
            coordinate: Coordinate,
            tileDef: TileDefinition,
            newTileDef: TileDefinition
        ): Boolean {
            if (baseLayer.swapTile(coordinate, tileDef, newTileDef)) {
                return true
            }
            return sparseLayer?.swapTile(coordinate, tileDef, newTileDef) ?: false
        }

        override fun replaceTiles(
            coordinate: Coordinate,
            tileDef: TileDefinition
        ): Boolean {
            if (baseLayer.replaceTiles(coordinate, tileDef)) {
                return true
            }
            return sparseLayer?.replaceTiles(coordinate, tileDef) ?: false
        }

        override fun removeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
            if (baseLayer.removeTile(coordinate, tileDef)) {
                return true
            }
            return sparseLayer?.removeTile(coordinate, tileDef) ?: false
        }

        override fun resetTile(coordinate: Coordinate) {
            baseLayer.resetTile(coordinate)
            sparseLayer?.resetTile(coordinate)
        }

        override fun annotateTile(
            coordinate: Coordinate,
            key: String,
            data: Map<Any, Any>?
        ) {
            baseLayer.annotateTile(coordinate, key, data)
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
            baseLayer = DenseMapLayer("default", registries).also {
                addLayer(it)
            }
        }
        if (sparseLayer == null) {
            sparseLayer = SparseMapLayer("default").also {
                addLayer(it)
            }
        }
    }

    fun getLayer(layerName: String): MapLayer {
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

    fun placeTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        val layer = getLayer(layerName ?: "default")
        val result = layer.placeTile(coordinate, tileDef)
        if (result) {
            notifyListeners(coordinate)
        }
        return result
    }

    fun swapTile(
        coordinate: Coordinate,
        oldTileDef: TileDefinition,
        newTileDef: TileDefinition,
        layerName: String? = null
    ): Boolean {
        val layer = getLayer(layerName ?: "default")
        val result = layer.swapTile(coordinate, oldTileDef, newTileDef)
        if (result) {
            notifyListeners(coordinate)
        }
        return result
    }

    fun replaceTiles(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        val layer = getLayer(layerName ?: "default")
        val result = layer.replaceTiles(coordinate, tileDef)
        if (result) {
            notifyListeners(coordinate)
        }
        return result
    }

    fun removeTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        val layer = getLayer(layerName ?: "default")
        val result = layer.removeTile(coordinate, tileDef)
        if (result) {
            notifyListeners(coordinate)
        }
        return result
    }

    fun resetTile(coordinate: Coordinate, layerName: String? = null) {
        val layer = getLayer(layerName ?: "default")
        layer.resetTile(coordinate)
        notifyListeners(coordinate)
    }

    fun annotateTile(coordinate: Coordinate, key: String, data: Map<Any, Any>?, layerName: String? = null) {
        val layer = getLayer(layerName ?: "default")
        layer.annotateTile(coordinate, key, data)
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

    fun applyOperation(operation: SparseOperation, layerName: String? = null) {
        when (operation) {
            is SparseTilePlacement -> {
                placeTile(
                    operation.coordinate,
                    operation.tileDef,
                    layerName
                )
            }

            is SparseTilesReplacement -> {
                replaceTiles(
                    operation.coordinate,
                    operation.tileDef,
                    layerName
                )
            }

            is SparseTileSwap -> {
                swapTile(operation.coordinate, operation.oldTileDef, operation.newTileDef, layerName)
            }

            is SparseTileAnnotation -> {
                annotateTile(operation.coordinate, operation.key, operation.data, layerName)
            }

            is SparseTileRemoval -> {
                removeTile(
                    operation.coordinate,
                    operation.tileDef,
                    layerName
                )
            }
        }
    }

    fun merge(other: MapTree) {
        other.layers.forEach { otherLayer ->
            when (otherLayer) {
                is DenseMapLayer -> {
                    otherLayer.chunks.forEach { (chunkCoordinate, chunk) ->
                        chunk.tiles.withIndex().forEach { (index, tileId) ->
                            val relativeX = index % chunk.size
                            val relativeY = index / chunk.size
                            val absoluteX = chunkCoordinate.x + relativeX
                            val absoluteY = chunkCoordinate.y + relativeY
                            val tileDef = registries.tiles.get(tileId) ?: return@forEach
                            placeTile(Coordinate(absoluteX, absoluteY, chunkCoordinate.z), tileDef)
                        }
                        chunk.annotations.cellSet().forEach {
                            annotateTile(
                                Coordinate(it.rowKey.x, it.rowKey.y, chunkCoordinate.z),
                                it.columnKey,
                                it.value
                            )
                        }
                    }
                }

                is SparseMapLayer -> {
                    otherLayer.chunks.forEach { (_, chunk) ->
                        chunk.operations.forEach { (_, operations) ->
                            operations.forEach { operation ->
                                applyOperation(operation)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Merges another MapTree into this one, copying all tiles and annotations.
         *
         * ```signatures
         * Merge(other: MapTree)
         * ```
         */
        private fun luaMerge(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val other = lua.checkUserdata<MapTree>(2)
            mapTree.merge(other)
            return 0
        }

        /**
         * Places a tile at the specified coordinate on the given layer.
         *
         * ```signatures
         * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition)
         * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
         * ```
         */
        private fun luaPlaceTile(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileDef = lua.checkRegistry(index + 1, mapTree.registries.tiles)
            val layerName = lua.toString(index + 2)
            mapTree.placeTile(coordinate, tileDef, layerName)
            return 0
        }

        /**
         * Replaces all tiles at the specified coordinate with the given tile.
         *
         * ```signatures
         * ReplaceTiles(coordinate: Coordinate, tileDef: TileDefinition)
         * ReplaceTiles(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
         * ```
         */
        private fun luaReplaceTiles(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileDef = lua.checkRegistry(index + 1, mapTree.registries.tiles)
            val layerName = lua.toString(index + 2)
            mapTree.replaceTiles(coordinate, tileDef, layerName)
            return 0
        }

        /**
         * Swaps one tile for another at the specified coordinate.
         *
         * ```signatures
         * SwapTile(coordinate: Coordinate, oldTileDef: TileDefinition, newTileDef: TileDefinition)
         * SwapTile(coordinate: Coordinate, oldTileDef: TileDefinition, newTileDef: TileDefinition, layerName: string)
         * ```
         */
        private fun luaSwapTile(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val oldTileDef = lua.checkRegistry(index + 1, mapTree.registries.tiles)
            val newTileDef = lua.checkRegistry(index + 2, mapTree.registries.tiles)
            val layerName = lua.toString(index + 3)
            mapTree.swapTile(coordinate, oldTileDef, newTileDef, layerName)
            return 0
        }

        /**
         * Removes a specific tile from the specified coordinate.
         *
         * ```signatures
         * RemoveTile(coordinate: Coordinate, tileDef: TileDefinition)
         * RemoveTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
         * ```
         */
        private fun luaRemoveTile(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileDef = lua.checkRegistry(index + 1, mapTree.registries.tiles)
            val layerName = lua.toString(index + 2)
            mapTree.removeTile(coordinate, tileDef, layerName)
            return 0
        }

        /**
         * Removes all tiles at the specified coordinate.
         *
         * ```signatures
         * ResetTile(coordinate: Coordinate)
         * ResetTile(coordinate: Coordinate, layerName: string)
         * ```
         */
        private fun luaResetTile(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val layerName = lua.toString(index + 1)
            mapTree.resetTile(coordinate, layerName)
            return 0
        }

        /**
         * Adds annotation data to a tile at the specified coordinate.
         *
         * ```signatures
         * AnnotateTile(coordinate: Coordinate, key: string, data: table)
         * AnnotateTile(coordinate: Coordinate, key: string, data: table, layerName: string)
         * ```
         */
        private fun luaAnnotateTile(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val key = lua.checkString(index + 1)
            val data = lua.checkAnyMap(index + 2)
            val layerName = lua.toString(index + 3)
            mapTree.annotateTile(coordinate, key, data, layerName)
            return 0
        }

        /**
         * Sets the visibility of a layer for a specific vision tag.
         *
         * ```signatures
         * SetVisibility(layerName: string, enabled: boolean)
         * SetVisibility(layerName: string, enabled: boolean, tag: string)
         * ```
         */
        private fun luaSetVisibility(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val enabled = lua.checkBoolean(3)
            val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
            if (enabled) {
                layer.addVisibilityTag(tagName)
            } else {
                layer.removeVisibilityTag(tagName)
            }
            return 0
        }

        /**
         * Checks if a layer is visible for a specific vision tag.
         *
         * ```signatures
         * IsVisible(layerName: string) -> boolean
         * IsVisible(layerName: string, tag: string) -> boolean
         * ```
         */
        private fun luaIsVisible(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(layer.visibilityTags.contains(tagName))
            return 1
        }

        /**
         * Checks if a layer is invisible for a specific vision tag.
         *
         * ```signatures
         * IsInvisible(layerName: string) -> boolean
         * IsInvisible(layerName: string, tag: string) -> boolean
         * ```
         */
        private fun luaIsInvisible(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(!layer.visibilityTags.contains(tagName))
            return 1
        }

        /**
         * Makes a layer visible for a specific vision tag.
         *
         * ```signatures
         * MakeVisible(layerName: string)
         * MakeVisible(layerName: string, tag: string)
         * ```
         */
        private fun luaMakeVisible(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.addVisibilityTag(tagName)
            return 0
        }

        /**
         * Makes a layer invisible for a specific vision tag.
         *
         * ```signatures
         * MakeInvisible(layerName: string)
         * MakeInvisible(layerName: string, tag: string)
         * ```
         */
        private fun luaMakeInvisible(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.removeVisibilityTag(tagName)
            return 0
        }

        /**
         * Checks if a layer has collision enabled for a specific tag.
         *
         * ```signatures
         * HasCollisions(layerName: string) -> boolean
         * HasCollisions(layerName: string, tag: string) -> boolean
         * ```
         */
        private fun luaHasCollisions(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            lua.push(layer.collisionTags.contains(tagName))
            return 0
        }

        /**
         * Sets collision state for a layer with a specific tag.
         *
         * ```signatures
         * SetCollisions(layerName: string, enabled: boolean)
         * SetCollisions(layerName: string, enabled: boolean, tag: string)
         * ```
         */
        private fun luaSetCollisions(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val enabled = lua.checkBoolean(3)
            val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
            if (enabled) {
                layer.addCollisionTag(tagName)
            } else {
                layer.removeCollisionTag(tagName)
            }
            return 0
        }

        /**
         * Enables collision for a layer with a specific tag.
         *
         * ```signatures
         * EnableCollisions(layerName: string)
         * EnableCollisions(layerName: string, tag: string)
         * ```
         */
        private fun luaEnableCollisions(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.addCollisionTag(tagName)
            return 0
        }

        /**
         * Disables collision for a layer with a specific tag.
         *
         * ```signatures
         * DisableCollisions(layerName: string)
         * DisableCollisions(layerName: string, tag: string)
         * ```
         */
        private fun luaDisableCollisions(lua: Lua): Int {
            val mapTree = lua.checkUserdata<MapTree>(1)
            val layerName = lua.checkString(2)
            val layer = mapTree.getLayer(layerName)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            layer.removeCollisionTag(tagName)
            return 0
        }

        val luaMeta = LuaMappedMetatable(MapTree::class) {
            callable(::luaMerge)
            callable(::luaPlaceTile)
            callable(::luaReplaceTiles)
            callable(::luaSwapTile)
            callable(::luaRemoveTile)
            callable(::luaResetTile)
            callable(::luaAnnotateTile)
            callable(::luaSetVisibility)
            callable(::luaMakeVisible)
            callable(::luaMakeInvisible)
            callable(::luaIsVisible)
            callable(::luaIsInvisible)
            callable(::luaSetCollisions)
            callable(::luaEnableCollisions)
            callable(::luaDisableCollisions)
            callable(::luaHasCollisions)
        }
    }

}
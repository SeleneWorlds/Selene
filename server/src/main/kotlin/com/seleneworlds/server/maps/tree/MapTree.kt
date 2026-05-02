package com.seleneworlds.server.maps.tree

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.layers.BaseMapLayer
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.server.maps.layers.EmptyMapLayer
import com.seleneworlds.server.maps.layers.MapLayer
import com.seleneworlds.server.maps.layers.SparseMapLayer
import com.seleneworlds.server.maps.layers.SparseOperation
import com.seleneworlds.server.maps.layers.SparseTileAnnotation
import com.seleneworlds.server.maps.layers.SparseTilePlacement
import com.seleneworlds.server.maps.layers.SparseTileRemoval
import com.seleneworlds.server.maps.layers.SparseTileSwap
import com.seleneworlds.server.maps.layers.SparseTilesReplacement

class MapTree(val registries: Registries) : ExposedApi<MapTreeApi> {
    override val api = MapTreeApi(this)
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
            data: SerializedMap?
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

    fun annotateTile(coordinate: Coordinate, key: String, data: SerializedMap?, layerName: String? = null) {
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
                operation.tileDef.get()?.let { tileDefinition ->
                    placeTile(
                        operation.coordinate,
                        tileDefinition,
                        layerName
                    )
                }
            }

            is SparseTilesReplacement -> {
                operation.tileDef.get()?.let { tileDefinition ->
                    replaceTiles(
                        operation.coordinate,
                        tileDefinition,
                        layerName
                    )
                }
            }

            is SparseTileSwap -> {
                operation.oldTileDef.get()?.let { oldTileDefinition ->
                    operation.newTileDef.get()?.let { newTileDefinition ->
                        swapTile(operation.coordinate, oldTileDefinition, newTileDefinition, layerName)
                    }
                }
            }

            is SparseTileAnnotation -> {
                annotateTile(operation.coordinate, operation.key, operation.data, layerName)
            }

            is SparseTileRemoval -> {
                operation.tileDef.get()?.let { tileDefinition ->
                    removeTile(
                        operation.coordinate,
                        tileDefinition,
                        layerName
                    )
                }
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
}

package com.seleneworlds.server.maps.tree

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.server.maps.layers.MapLayerApi

class MapTreeApi(val mapTree: MapTree) {

    /** Checks for a named layer without creating it. */
    fun hasLayer(layerName: String): Boolean = mapTree.layers.any { it.name == layerName }

    /** Returns the named layer, creating it if needed. Layers can be saved directly. */
    fun getLayer(layerName: String): MapLayerApi {
        require(layerName != "default") { "Select an explicit named layer" }
        return MapLayerApi(mapTree.getLayer(layerName), mapTree.registries)
    }

    /** Attaches a layer above the existing layers. */
    fun addLayer(layer: MapLayerApi) {
        require(layer.registries === mapTree.registries) { "Layer belongs to different registries" }
        require(layer.getName() != "default") { "Select an explicit named layer" }
        require(mapTree.layers.none { it.name == layer.getName() }) { "Layer already exists: ${layer.getName()}" }
        mapTree.addLayer(layer.layer)
    }

    fun merge(other: MapTreeApi) {
        mapTree.merge(other.mapTree)
    }

    fun placeTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        return mapTree.placeTile(coordinate, tileDef, layerName)
    }

    fun replaceTiles(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        return mapTree.replaceTiles(coordinate, tileDef, layerName)
    }

    fun swapTile(
        coordinate: Coordinate,
        oldTileDef: TileDefinition,
        newTileDef: TileDefinition,
        layerName: String? = null
    ): Boolean {
        return mapTree.swapTile(coordinate, oldTileDef, newTileDef, layerName)
    }

    fun removeTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: String? = null): Boolean {
        return mapTree.removeTile(coordinate, tileDef, layerName)
    }

    fun resetTile(coordinate: Coordinate, layerName: String? = null) {
        mapTree.resetTile(coordinate, layerName)
    }

    fun annotateTile(coordinate: Coordinate, key: String, data: SerializedMap?, layerName: String? = null) {
        mapTree.annotateTile(coordinate, key, data, layerName)
    }

    fun setVisibility(layerName: String, enabled: Boolean, tagName: String = "default") {
        val layer = mapTree.getLayer(layerName)
        if (enabled) {
            layer.addVisibilityTag(tagName)
        } else {
            layer.removeVisibilityTag(tagName)
        }
    }

    fun isVisible(layerName: String, tagName: String = "default"): Boolean {
        return mapTree.getLayer(layerName).visibilityTags.contains(tagName)
    }

    fun isInvisible(layerName: String, tagName: String = "default"): Boolean {
        return !isVisible(layerName, tagName)
    }

    fun makeVisible(layerName: String, tagName: String = "default") {
        mapTree.getLayer(layerName).addVisibilityTag(tagName)
    }

    fun makeInvisible(layerName: String, tagName: String = "default") {
        mapTree.getLayer(layerName).removeVisibilityTag(tagName)
    }

    fun hasCollisions(layerName: String, tagName: String = "default"): Boolean {
        return mapTree.getLayer(layerName).collisionTags.contains(tagName)
    }

    fun setCollisions(layerName: String, enabled: Boolean, tagName: String = "default") {
        val layer = mapTree.getLayer(layerName)
        if (enabled) {
            layer.addCollisionTag(tagName)
        } else {
            layer.removeCollisionTag(tagName)
        }
    }

    fun enableCollisions(layerName: String, tagName: String = "default") {
        mapTree.getLayer(layerName).addCollisionTag(tagName)
    }

    fun disableCollisions(layerName: String, tagName: String = "default") {
        mapTree.getLayer(layerName).removeCollisionTag(tagName)
    }
}

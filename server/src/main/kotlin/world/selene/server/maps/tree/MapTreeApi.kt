package world.selene.server.maps.tree

import world.selene.common.grid.Coordinate
import world.selene.common.tiles.TileDefinition

class MapTreeApi(val mapTree: MapTree) {

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

    fun annotateTile(coordinate: Coordinate, key: String, data: Map<Any, Any>?, layerName: String? = null) {
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

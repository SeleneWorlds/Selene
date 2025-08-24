package world.selene.server.maps

import world.selene.common.data.TileDefinition
import world.selene.common.util.Coordinate

object EmptyMapLayer : MapLayer, BaseMapLayer {
    override val name: String = "empty"
    override val visibilityTags: Set<String> = emptySet()
    override val collisionTags: Set<String> = emptySet()

    override fun placeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        return false
    }

    override fun swapTile(
        coordinate: Coordinate,
        tileDef: TileDefinition,
        newTileDef: TileDefinition
    ): Boolean {
        return false
    }

    override fun replaceTiles(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        return false
    }

    override fun removeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        return false
    }

    override fun resetTile(coordinate: Coordinate) {
    }

    override fun annotateTile(
        coordinate: Coordinate,
        key: String,
        data: Map<*, *>
    ) {
    }

    override fun addVisibilityTag(tagName: String) {
    }

    override fun removeVisibilityTag(tagName: String) {
    }

    override fun addCollisionTag(tagName: String) {
    }

    override fun removeCollisionTag(tagName: String) {
    }

    override fun getTileId(coordinate: Coordinate): Int {
        return 0
    }
}
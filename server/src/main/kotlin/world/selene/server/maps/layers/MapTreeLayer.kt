package world.selene.server.maps.layers

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import world.selene.common.tiles.TileDefinition
import world.selene.common.grid.Coordinate
import world.selene.server.maps.tree.MapTree

class MapTreeLayer(override val name: String, private val mapTree: MapTree) : MapLayer, BaseMapLayer {
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    fun getBaseTile(coordinate: Coordinate): Int {
        return 0 // TODO access mapTree for this
    }

    fun getAdditionalTiles(coordinate: Coordinate): List<Int> {
        return emptyList()  // TODO access mapTree for this
    }

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

    override fun replaceTiles(
        coordinate: Coordinate,
        tileDef: TileDefinition
    ): Boolean {
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
        data: Map<Any, Any>?
    ) {
    }

    override fun getTileId(coordinate: Coordinate): Int {
        return 0
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

    fun getAnnotations(): Table<Coordinate, String, Map<*, *>> {
        return HashBasedTable.create() // TODO access mapTree for this
    }
}
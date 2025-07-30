package world.selene.server.maps

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import world.selene.common.util.Coordinate

class MapTreeLayer(override val name: String, private val mapTree: MapTree) : MapLayer, BaseMapLayer {
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    fun getBaseTile(x: Int, y: Int, z: Int): Int {
        return 0 // TODO access mapTree for this
    }

    fun getBaseTile(coordinate: Coordinate): Int {
        return getBaseTile(coordinate.x, coordinate.y, coordinate.z)
    }

    fun getAdditionalTiles(x: Int, y: Int, z: Int): List<Int> {
        return emptyList()  // TODO access mapTree for this
    }

    fun getAdditionalTiles(coordinate: Coordinate): List<Int> {
        return getAdditionalTiles(coordinate.x, coordinate.y, coordinate.z)
    }

    override fun placeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        return false
    }

    override fun replaceTiles(
        x: Int,
        y: Int,
        z: Int,
        tileId: Int
    ): Boolean {
        return false
    }

    override fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        return false
    }

    override fun resetTile(x: Int, y: Int, z: Int) {
    }

    override fun annotateTile(
        x: Int,
        y: Int,
        z: Int,
        key: String,
        data: Map<*, *>
    ) {
    }

    override fun getTileId(x: Int, y: Int, z: Int): Int {
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
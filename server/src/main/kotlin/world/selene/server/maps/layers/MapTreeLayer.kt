package world.selene.server.maps.layers

import world.selene.common.grid.ChunkWindow
import world.selene.common.tiles.TileDefinition
import world.selene.common.grid.Coordinate
import world.selene.server.cameras.viewer.DefaultViewer
import world.selene.server.maps.tree.MapTree
import world.selene.server.sync.ScopedChunkView

/**
 * TODO This should use a cached view of the map tree. It can precompute and store data by chunks.
 */
class MapTreeLayer(override val name: String, private val mapTree: MapTree) : MapLayer, BaseMapLayer {
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    fun getAdditionalTiles(coordinate: Coordinate): List<Int> {
        val view = ScopedChunkView.create(mapTree, DefaultViewer, ChunkWindow.at(coordinate, 1))
        return view.getAdditionalTilesAt(coordinate)
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
        val view = ScopedChunkView.create(mapTree, DefaultViewer, ChunkWindow.at(coordinate, 1))
        return view.getBaseTileAt(coordinate)
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

    fun getAnnotations(coordinate: Coordinate): Map<String, Map<*, *>> {
        val view = ScopedChunkView.create(mapTree, DefaultViewer, ChunkWindow.at(coordinate, 1))
        return view.getAnnotationsAt(coordinate)
    }
}
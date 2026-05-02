package world.selene.server.sync

import world.selene.common.grid.ChunkWindow
import world.selene.common.grid.Coordinate

class ScopedChunkViewApi(val chunkView: ScopedChunkView) {

    fun getWindow(): ChunkWindow {
        return chunkView.window
    }

    fun getBaseTileAtRelative(ox: Int, oy: Int): Int {
        return chunkView.getBaseTileAtRelative(ox, oy)
    }

    fun getBaseTileAt(coordinate: Coordinate): Int {
        return chunkView.getBaseTileAt(coordinate)
    }

    fun getAdditionalTilesAt(coordinate: Coordinate): List<Int> {
        return chunkView.getAdditionalTilesAt(coordinate)
    }

    fun getAnnotationsAt(coordinate: Coordinate): Map<String, Map<*, *>> {
        return chunkView.getAnnotationsAt(coordinate)
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String): Map<*, *>? {
        return chunkView.getAnnotationAt(coordinate, key)
    }
}

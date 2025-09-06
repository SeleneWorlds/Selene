package world.selene.server.sync

import world.selene.common.util.ChunkWindow
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Viewer
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.TransitionResolver

class ChunkViewManager(private val transitionResolver: TransitionResolver) {

    val chunkSize = 16

    fun atCoordinate(dimension: Dimension, viewer: Viewer, coordinate: Coordinate): ScopedChunkView {
        return ScopedChunkView.create(dimension, viewer, ChunkWindow.at(coordinate, chunkSize)).apply {
            transitionResolver.applyTransitions(this)
        }
    }

    fun atWindow(dimension: Dimension, viewer: Viewer, window: ChunkWindow): ScopedChunkView {
        return ScopedChunkView.create(dimension, viewer, window).apply {
            transitionResolver.applyTransitions(this)
        }
    }
}
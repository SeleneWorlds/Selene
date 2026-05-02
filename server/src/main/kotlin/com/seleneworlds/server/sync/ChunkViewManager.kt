package com.seleneworlds.server.sync

import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.tiles.transitions.TransitionResolver

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
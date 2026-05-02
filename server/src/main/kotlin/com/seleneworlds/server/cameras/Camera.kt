package com.seleneworlds.server.cameras

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.maps.layers.MapLayer

class Camera : Viewer {
    var dimension: Dimension? = null
        private set(value) {
            val prev = field
            field = value
            if (prev != value) {
                listeners.forEach { it.cameraDimensionChanged(this, prev, value) }
            }
        }

    var coordinate = Coordinate.Zero
        private set(value) {
            val prev = field
            field = value
            if (prev != value) {
                listeners.forEach { it.cameraCoordinateChanged(this, prev, value) }
            }
        }
    var visionEntity: Entity? = null; private set
    var followEntity: Entity? = null; private set
    val listeners = mutableListOf<CameraListener>()

    val viewer get() = visionEntity?.visionViewer ?: DefaultViewer

    fun followEntity(entity: Entity) {
        followEntity = entity
        visionEntity = entity
        dimension = entity.dimension
        coordinate = entity.coordinate
    }

    fun focusCoordinate(dimension: Dimension, coordinate: Coordinate) {
        followEntity = null
        this.dimension = dimension
        this.coordinate = coordinate
    }

    fun update() {
        followEntity?.let {
            dimension = it.dimension
            coordinate = it.coordinate
        }
    }

    fun addListener(listener: CameraListener) {
        listeners.add(listener)
    }

    override fun canView(layer: MapLayer): Boolean {
        return viewer.canView(layer)
    }

    override fun canView(entity: Entity): Boolean {
        return viewer.canView(entity)
    }
}
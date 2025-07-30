package world.selene.server.cameras

import world.selene.common.util.Coordinate
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.Entity
import world.selene.server.maps.MapLayer

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
        dimension = entity.dimension
        followEntity = entity
        visionEntity = entity
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
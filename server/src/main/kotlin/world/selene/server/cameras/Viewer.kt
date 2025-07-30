package world.selene.server.cameras

import world.selene.server.entities.Entity
import world.selene.server.maps.MapLayer

interface Viewer {
    fun canView(layer: MapLayer): Boolean
    fun canView(entity: Entity): Boolean
}
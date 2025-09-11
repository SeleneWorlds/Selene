package world.selene.server.cameras.viewer

import world.selene.server.entities.Entity
import world.selene.server.maps.layers.MapLayer

interface Viewer {
    fun canView(layer: MapLayer): Boolean
    fun canView(entity: Entity): Boolean
}
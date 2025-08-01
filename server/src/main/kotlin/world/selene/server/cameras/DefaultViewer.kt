package world.selene.server.cameras

import world.selene.server.entities.Entity
import world.selene.server.maps.MapLayer

object DefaultViewer : Viewer {
    override fun canView(layer: MapLayer): Boolean {
        return layer.visibilityTags.contains("default")
    }

    override fun canView(entity: Entity): Boolean {
        return entity.visibilityTags.contains("default")
    }
}

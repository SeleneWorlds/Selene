package com.seleneworlds.server.cameras.viewer

import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.maps.layers.MapLayer

object DefaultViewer : Viewer {
    override fun canView(layer: MapLayer): Boolean {
        return layer.visibilityTags.contains("default")
    }

    override fun canView(entity: Entity): Boolean {
        return entity.visibilityTags.contains("default")
    }
}

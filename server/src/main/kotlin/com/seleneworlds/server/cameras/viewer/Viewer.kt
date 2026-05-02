package com.seleneworlds.server.cameras.viewer

import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.maps.layers.MapLayer

interface Viewer {
    fun canView(layer: MapLayer): Boolean
    fun canView(entity: Entity): Boolean
}
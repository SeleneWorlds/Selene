package com.seleneworlds.server.maps.layers

import com.seleneworlds.server.data.Registries

/** A handle to an actual map layer, independent of the save format. */
class MapLayerApi(val layer: MapLayer, internal val registries: Registries) {
    fun getName(): String = layer.name
}

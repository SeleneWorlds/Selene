package com.seleneworlds.server.saves

import com.seleneworlds.server.maps.tree.MapTree
import java.io.File
import com.seleneworlds.server.maps.layers.MapLayerApi

interface MapTreeFormat {
    fun loadMapTree(file: File): MapTree
    fun loadMapLayer(file: File): MapLayerApi
    fun saveLayer(file: File, layer: MapLayerApi)
    fun saveFullyInline(file: File, mapTree: MapTree)
}

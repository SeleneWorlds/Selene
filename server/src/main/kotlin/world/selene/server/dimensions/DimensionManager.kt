package world.selene.server.dimensions

import world.selene.server.maps.MapManager

class DimensionManager(private val mapManager: MapManager) {
    val dimensions = mutableMapOf<Int, Dimension>()

    init {
        getOrCreateDimension(0)
    }

    fun getOrCreateDimension(id: Int): Dimension {
        return dimensions.getOrPut(id) { Dimension(mapManager.createMapTree()) }
    }
}
package world.selene.server.dimensions

import org.koin.mp.KoinPlatform.getKoin

class DimensionManager() {
    val dimensions = mutableMapOf<Int, Dimension>()

    fun getOrCreateDimension(id: Int): Dimension {
        return dimensions.getOrPut(id) { getKoin().get<Dimension>() }
    }
}
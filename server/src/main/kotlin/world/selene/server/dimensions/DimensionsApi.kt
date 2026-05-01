package world.selene.server.dimensions

class DimensionsApi(val dimensionManager: DimensionManager) {
    fun getDefault(): DimensionApi {
        return dimensionManager.getOrCreateDimension(0).api
    }
}

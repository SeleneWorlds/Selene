package com.seleneworlds.server.dimensions

class DimensionsApi(val dimensionManager: DimensionManager) {
    fun getDefault(): DimensionApi {
        return dimensionManager.getOrCreateDimension(0).api
    }
}

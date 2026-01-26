package world.selene.client.assets

/**
 * Interface for listening to asset reload events.
 * Implementations can register with AssetProvider to be notified
 * when assets are loaded, reloaded, or fail to load.
 */
interface AssetReloadListener {
    /**
     * Called when an asset subscription is triggered (asset changed notification).
     * 
     * @param assetPath The path of the asset that changed
     */
    fun onAssetChanged(assetPath: String) = Unit
}

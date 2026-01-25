package world.selene.client.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.assets.async.AssetLoadingException
import ktx.assets.async.AssetStorage
import org.slf4j.Logger
import world.selene.client.rendering.drawable.ReloadableTextureRegion
import world.selene.common.util.Disposable
import java.util.concurrent.ConcurrentHashMap

class AssetProvider(private val logger: Logger, private val assetStorage: AssetStorage) : Disposable {

    val missingTexture = TextureRegion(Texture(1, 1, Pixmap.Format.RGBA8888))

    private val assetSubscriptions = ConcurrentHashMap<String, MutableSet<(String) -> Unit>>()
    private val reloadListeners = mutableSetOf<AssetReloadListener>()

    fun loadTexture(texturePath: String): Texture? {
        try {
            return assetStorage.loadSync<Texture>(texturePath)
        } catch (e: AssetLoadingException) {
            logger.error("Failed to load texture $texturePath", e)
            return null
        }
    }

    fun loadReloadableTextureRegion(
        texturePath: String,
        flipX: Boolean = false,
        flipY: Boolean = false
    ): ReloadableTextureRegion {
        return ReloadableTextureRegion(this, texturePath, flipX, flipY)
    }

    fun subscribeToAssetChanges(assetPath: String, callback: (String) -> Unit) {
        assetSubscriptions.computeIfAbsent(assetPath) { ConcurrentHashMap.newKeySet() }.add(callback)
    }

    fun unsubscribeFromAssetChanges(assetPath: String, callback: (String) -> Unit) {
        assetSubscriptions[assetPath]?.remove(callback)
    }

    fun notifyAssetChanged(assetPath: String) {
        assetSubscriptions[assetPath]?.forEach { callback ->
            callback(assetPath)
        }
        notifyAssetChangedListeners(assetPath)
    }

    override fun dispose() {
        assetStorage.dispose()
        assetSubscriptions.clear()
        reloadListeners.clear()
    }

    /**
     * Registers a reload listener to receive notifications about asset changes.
     */
    fun addReloadListener(listener: AssetReloadListener) {
        reloadListeners.add(listener)
    }

    /**
     * Removes a previously registered reload listener.
     */
    fun removeReloadListener(listener: AssetReloadListener) {
        reloadListeners.remove(listener)
    }

    private fun notifyAssetChangedListeners(assetPath: String) {
        reloadListeners.forEach { listener ->
            listener.onAssetChanged(assetPath)
        }
    }

}
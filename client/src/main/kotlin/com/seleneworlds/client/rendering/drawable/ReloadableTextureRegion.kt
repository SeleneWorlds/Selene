package com.seleneworlds.client.rendering.drawable

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.seleneworlds.common.threading.Awaitable
import com.seleneworlds.common.threading.MainThreadDispatcher
import kotlinx.coroutines.Deferred
import com.seleneworlds.client.assets.AssetProvider

class ReloadableTextureRegion(
    private val assetProvider: AssetProvider,
    private val mainThreadDispatcher: MainThreadDispatcher,
    private val texturePath: String,
    private val flipX: Boolean = false,
    private val flipY: Boolean = false
) : TextureRegion() {
    private var pendingTextureLoad: Deferred<*>? = null
    private var appliedTexture: Int? = null
    private var initializeAwaitable: Awaitable<Void?>? = null

    val reloadCallback = { _: String -> reload() }

    init {
        setRegion(assetProvider.missingTexture)
        assetProvider.subscribeToAssetChanges(texturePath, reloadCallback)
    }

    fun reload() {
        pendingTextureLoad = assetProvider.loadTextureAsync(texturePath)
        appliedTexture = null
        setRegion(assetProvider.missingTexture)
    }

    fun initialize(): Awaitable<Void?> {
        initializeAwaitable?.let { return it }

        val future = Awaitable<Void?>()
        initializeAwaitable = future

        if (pendingTextureLoad == null) {
            reload()
        }

        val pendingTextureLoad = pendingTextureLoad
        if (pendingTextureLoad == null) {
            future.resolve(null)
            return future
        }

        pendingTextureLoad.invokeOnCompletion { error ->
            if (error != null) {
                future.reject(error)
                return@invokeOnCompletion
            }

            mainThreadDispatcher.runOnMainThread {
                try {
                    update()
                    future.resolve(null)
                } catch (e: Exception) {
                    future.reject(e)
                }
            }
        }

        return future
    }

    fun update() {
        val pendingTextureLoad = pendingTextureLoad ?: return
        if (!pendingTextureLoad.isCompleted) {
            return
        }

        this.pendingTextureLoad = null
        val texture = assetProvider.getLoadedTexture(texturePath)
        if (texture != null) {
            applyTexture(texture)
        } else {
            setRegion(assetProvider.missingTexture)
        }
    }

    private fun applyTexture(texture: Texture) {
        val textureIdentity = System.identityHashCode(texture)
        val oldTextureIdentity = appliedTexture
        val shouldFlip = oldTextureIdentity == null || oldTextureIdentity != textureIdentity

        setRegion(texture)
        if (shouldFlip && (flipX || flipY)) {
            flip(flipX, flipY)
        }
        appliedTexture = textureIdentity
    }

    fun dispose() {
        assetProvider.unsubscribeFromAssetChanges(texturePath, reloadCallback)
    }

    override fun toString(): String {
        return "ReloadableTextureRegion(path='$texturePath')"
    }
}

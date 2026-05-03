package com.seleneworlds.client.rendering.drawable

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlinx.coroutines.Deferred
import com.seleneworlds.client.assets.AssetProvider

class ReloadableTextureRegion(
    private val assetProvider: AssetProvider,
    private val texturePath: String,
    private val flipX: Boolean = false,
    private val flipY: Boolean = false
) : TextureRegion() {
    private var pendingTextureLoad: Deferred<*>? = null
    private var appliedTexture: Int? = null

    val reloadCallback = { _: String -> reload() }

    init {
        reload()
        assetProvider.subscribeToAssetChanges(texturePath, reloadCallback)
    }

    fun reload() {
        pendingTextureLoad = assetProvider.loadTextureAsync(texturePath)
        appliedTexture = null
        setRegion(assetProvider.missingTexture)
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

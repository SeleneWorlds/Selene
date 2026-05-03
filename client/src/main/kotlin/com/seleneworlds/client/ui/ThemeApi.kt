package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.rendering.texture.ScriptableTexture
import java.util.concurrent.CompletableFuture

class ThemeApi(
    internal val skin: Skin,
    private val skinResolvers: SkinResolvers,
    private val assetProvider: AssetProvider
) {
    fun addTexture(name: String, texturePath: String): CompletableFuture<Void?> {
        val textureFile = skinResolvers.resolveFile(texturePath)
        if (!textureFile.exists()) {
            return CompletableFuture.failedFuture(IllegalArgumentException("Texture file not found: $texturePath"))
        }

        val future = CompletableFuture<Void?>()
        assetProvider.loadTextureAsync(texturePath).invokeOnCompletion { error ->
            if (error != null) {
                future.completeExceptionally(error)
                return@invokeOnCompletion
            }

            val texture = assetProvider.getLoadedTexture(texturePath)
            if (texture == null) {
                future.completeExceptionally(IllegalStateException("Texture failed to load: $texturePath"))
                return@invokeOnCompletion
            }

            skin.add(name, TextureRegion(texture))
            future.complete(null)
        }
        return future
    }

    fun addTexture(name: String, texture: ScriptableTexture): CompletableFuture<Void?> {
        skin.add(name, TextureRegion(texture.texture))
        return CompletableFuture.completedFuture(null)
    }
}

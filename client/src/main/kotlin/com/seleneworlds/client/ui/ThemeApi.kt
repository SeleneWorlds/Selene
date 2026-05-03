package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.rendering.texture.ScriptableTexture
import com.seleneworlds.common.threading.Awaitable

class ThemeApi(
    internal val skin: Skin,
    private val skinResolvers: SkinResolvers,
    private val assetProvider: AssetProvider
) {
    fun addTexture(name: String, texturePath: String): Awaitable<Void?> {
        val textureFile = skinResolvers.resolveFile(texturePath)
        if (!textureFile.exists()) {
            return Awaitable.failed(IllegalArgumentException("Texture file not found: $texturePath"))
        }

        val future = Awaitable<Void?>()
        assetProvider.loadTextureAsync(texturePath).invokeOnCompletion { error ->
            if (error != null) {
                future.reject(error)
                return@invokeOnCompletion
            }

            val texture = assetProvider.getLoadedTexture(texturePath)
            if (texture == null) {
                future.reject(IllegalStateException("Texture failed to load: $texturePath"))
                return@invokeOnCompletion
            }

            skin.add(name, TextureRegion(texture))
            future.resolve(null)
        }
        return future
    }

    fun addTexture(name: String, texture: ScriptableTexture): Awaitable<Void?> {
        skin.add(name, TextureRegion(texture.texture))
        return Awaitable.completed(null)
    }
}

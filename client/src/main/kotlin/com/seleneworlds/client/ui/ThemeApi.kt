package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.seleneworlds.client.rendering.texture.ScriptableTexture
import java.util.concurrent.CompletableFuture

class ThemeApi(
    internal val skin: Skin,
    private val skinResolvers: SkinResolvers
) {
    fun addTexture(name: String, texturePath: String): CompletableFuture<Void?> {
        val textureFile = skinResolvers.resolveFile(texturePath)
        if (!textureFile.exists()) {
            return CompletableFuture.failedFuture(IllegalArgumentException("Texture file not found: $texturePath"))
        }

        skin.add(name, TextureRegion(Texture(textureFile)))
        return CompletableFuture.completedFuture(null)
    }

    fun addTexture(name: String, texture: ScriptableTexture): CompletableFuture<Void?> {
        skin.add(name, TextureRegion(texture.texture))
        return CompletableFuture.completedFuture(null)
    }
}

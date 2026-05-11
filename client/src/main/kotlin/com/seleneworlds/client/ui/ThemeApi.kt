package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.rendering.texture.ScriptableTexture
import com.seleneworlds.common.threading.Awaitable

class ThemeApi(
    internal val skin: Skin,
    private val assetProvider: AssetProvider
) {
    fun addTexture(name: String, texturePath: String): Awaitable<Void?> {
        val textureRegion = assetProvider.loadReloadableTextureRegion(texturePath)
        skin.add(name, textureRegion)
        return textureRegion.initialize()
    }

    fun addTexture(name: String, texture: ScriptableTexture): Awaitable<Void?> {
        skin.add(name, TextureRegion(texture.texture))
        return Awaitable.completed(null)
    }
}

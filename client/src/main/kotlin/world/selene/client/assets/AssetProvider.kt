package world.selene.client.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.assets.async.AssetLoadingException
import ktx.assets.async.AssetStorage
import org.slf4j.Logger
import world.selene.client.visual.TextureOptions

class AssetProvider(private val logger: Logger, private val assetStorage: AssetStorage) {

    val missingTexture = TextureRegion(Texture(1, 1, Pixmap.Format.RGBA8888))

    fun loadTextureRegion(texturePath: String, options: TextureOptions = TextureOptions.Default): TextureRegion? {
        try {
            val texture = assetStorage.loadSync<Texture>(texturePath)
            return TextureRegion(texture).apply { flip(options.flipX, !options.flipY) }
        } catch (e: AssetLoadingException) {
            logger.error("Failed to load texture $texturePath", e)
            return null
        }
    }

}
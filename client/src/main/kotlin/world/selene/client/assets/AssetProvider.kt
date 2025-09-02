package world.selene.client.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.assets.async.AssetLoadingException
import ktx.assets.async.AssetStorage
import org.slf4j.Logger
import world.selene.common.util.Disposable

class AssetProvider(private val logger: Logger, private val assetStorage: AssetStorage) : Disposable {

    val missingTexture = TextureRegion(Texture(1, 1, Pixmap.Format.RGBA8888))

    fun loadTexture(texturePath: String): Texture? {
        try {
            return assetStorage.loadSync<Texture>(texturePath)
        } catch (e: AssetLoadingException) {
            logger.error("Failed to load texture $texturePath", e)
            return null
        }
    }

    override fun dispose() {
        assetStorage.dispose()
    }
}
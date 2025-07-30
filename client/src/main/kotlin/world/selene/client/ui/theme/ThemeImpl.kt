package world.selene.client.ui.theme

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.assets.async.AssetStorage

class ThemeImpl(val assetStorage: AssetStorage) : Theme {
    override fun texture(name: String): TextureRegion {
        return TextureRegion(assetStorage.loadSync<Texture>(name)).apply { flip(false, true) }
    }
}
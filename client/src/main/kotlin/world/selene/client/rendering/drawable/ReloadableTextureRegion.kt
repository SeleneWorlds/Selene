package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.TextureRegion
import world.selene.client.assets.AssetProvider

class ReloadableTextureRegion(
    private val assetProvider: AssetProvider,
    private val texturePath: String,
    private val flipX: Boolean = false,
    private val flipY: Boolean = false
) : TextureRegion() {

    init {
        reload(texturePath)
        assetProvider.subscribeToAssetChanges(texturePath, ::reload)
    }

    fun reload(texturePath: String) {
        val texture = assetProvider.loadTexture(texturePath)
        if (texture != null) {
            setRegion(texture)
            flip(flipX, flipY)
        } else {
            setRegion(assetProvider.missingTexture)
        }
    }

    fun dispose() {
        assetProvider.unsubscribeFromAssetChanges(texturePath, ::reload)
    }

    override fun toString(): String {
        return "ReloadableTextureRegion(path='$texturePath')"
    }
}

package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.TextureRegion

class TextureRegionDrawableApi(val textureRegionDrawable: TextureRegionDrawable) : DrawableApi(textureRegionDrawable) {

    fun getTextureRegion(): TextureRegion {
        return textureRegionDrawable.textureRegion
    }

    fun withoutOffset(): TextureRegionDrawableApi {
        return textureRegionDrawable.withoutOffset().api
    }
}

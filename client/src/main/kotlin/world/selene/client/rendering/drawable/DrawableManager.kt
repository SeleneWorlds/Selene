package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.TextureRegion
import world.selene.client.assets.AssetProvider
import world.selene.common.util.Disposable

class DrawableManager(private val assetProvider: AssetProvider) : Disposable {
    private val cache = mutableMapOf<Pair<String, DrawableOptions>, Drawable>()
    private val animatedDrawables = mutableMapOf<String, AnimatedDrawable>()

    private fun getMissingDrawable(): TextureRegionDrawable {
        return TextureRegionDrawable(assetProvider.missingTexture, 0f, 0f)
    }

    private fun createDrawable(texture: String, options: DrawableOptions): Drawable? {
        val texture = assetProvider.loadTexture(texture) ?: return null
        val textureRegion = TextureRegion(texture)
        textureRegion.flip(options.flipX, !options.flipY)
        // TODO Test textures currently have their offsets configured with an origin
        val hackyOffsetX = options.offsetX - textureRegion.regionWidth / 2f
        val hackyOffsetY = -options.offsetY - textureRegion.regionHeight
        return TextureRegionDrawable(textureRegion, hackyOffsetX, hackyOffsetY)
    }

    fun getDrawable(texture: String, options: DrawableOptions): Drawable? {
        return cache.compute(texture to options) { (texture, options), value ->
            value ?: createDrawable(texture, options)
        }
    }

    private fun createAnimatedDrawable(
        frames: List<Pair<String, DrawableOptions>>,
        options: AnimatedDrawableOptions
    ): AnimatedDrawable {
        return AnimatedDrawable(
            frames.map { getDrawable(it.first, it.second) ?: getMissingDrawable() },
            options.duration
        )
    }

    fun getAnimatedDrawable(
        frames: List<Pair<String, DrawableOptions>>,
        options: AnimatedDrawableOptions,
        managedKey: String? = null
    ): AnimatedDrawable {
        if (managedKey == null) {
            return createAnimatedDrawable(frames, options)
        }
        return animatedDrawables.getOrPut(managedKey) {
            createAnimatedDrawable(frames, options)
        }
    }

    fun update(delta: Float) {
        animatedDrawables.values.forEach { it.update(delta) }
    }

    override fun dispose() {}

}
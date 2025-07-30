package world.selene.client.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion

class ImageNode : Node() {

    var src: String = ""
        set(value) {
            field = value
            themeChanged()
        }

    private var textureRegion: TextureRegion? = null

    override fun themeChanged() {
        textureRegion = theme.texture(src)
    }

    override fun fitToContent() {
        width = textureRegion?.regionWidth?.toFloat() ?: 0f
        height = textureRegion?.regionHeight?.toFloat() ?: 0f
    }

    override fun renderBackground(spriteBatch: SpriteBatch) {
        textureRegion?.let { spriteBatch.draw(it, absolutePosition.x, absolutePosition.y) }
    }

    override fun toString(): String {
        return "ImageNode(src=$src)"
    }
}
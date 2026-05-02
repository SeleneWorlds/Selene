package world.selene.client.rendering.texture

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

data class ScriptableTexture(val texture: Texture, val pixmap: Pixmap) {
    val width: Int get() = texture.width
    val height: Int get() = texture.height
}
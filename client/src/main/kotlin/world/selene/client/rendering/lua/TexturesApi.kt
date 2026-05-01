package world.selene.client.rendering.lua

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/**
 * Create and manipulate textures.
 */
@Suppress("SameReturnValue")
class TexturesApi {
    fun createTexture(width: Int, height: Int, formatName: String = "RGBA8888"): ScriptableTexture {
        val format = when (formatName) {
            "RGBA8888" -> Pixmap.Format.RGBA8888
            "RGB888" -> Pixmap.Format.RGB888
            "RGBA4444" -> Pixmap.Format.RGBA4444
            "RGB565" -> Pixmap.Format.RGB565
            "Alpha" -> Pixmap.Format.Alpha
            else -> Pixmap.Format.RGBA8888
        }

        val pixmap = Pixmap(width, height, format)
        val texture = Texture(pixmap)
        return ScriptableTexture(texture, pixmap)
    }
}

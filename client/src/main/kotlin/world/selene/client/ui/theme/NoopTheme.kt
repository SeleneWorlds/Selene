package world.selene.client.ui.theme

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

object NoopTheme : Theme {

    val emptyTexture = Texture(1, 1, Pixmap.Format.RGBA8888)
    val emptyTextureRegion = TextureRegion(emptyTexture)

    override fun texture(name: String): TextureRegion {
        return emptyTextureRegion
    }
}
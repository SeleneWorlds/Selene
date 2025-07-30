package world.selene.client.ui.theme

import com.badlogic.gdx.graphics.g2d.TextureRegion

interface Theme {
    fun texture(name: String): TextureRegion
}
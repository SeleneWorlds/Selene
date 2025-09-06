package world.selene.client.rendering.drawable

import com.badlogic.gdx.utils.Align

data class TextDrawableOptions(
    val horizontalAlign: Int = Align.left,
    val maxWidth: Float = 0f,
    val wrap: Boolean = false
)
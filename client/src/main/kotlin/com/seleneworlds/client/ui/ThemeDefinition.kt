package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTextField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThemeDefinition(
    @SerialName("ImageButton")
    val imageButtons: Map<String, ImageButtonThemeDefinition> = emptyMap(),
    @SerialName("Button")
    val buttons: Map<String, ButtonThemeDefinition> = emptyMap(),
    @SerialName("Label")
    val labels: Map<String, LabelThemeDefinition> = emptyMap(),
    @SerialName("TextField")
    val textFields: Map<String, TextFieldThemeDefinition> = emptyMap(),
    @SerialName("ProgressBar")
    val progressBars: Map<String, ProgressBarThemeDefinition> = emptyMap()
) {
    fun applyToSkin(skin: Skin, skinResolvers: SkinResolvers) {
        imageButtons.forEach { (name, definition) ->
            val up = definition.up?.let { skinResolvers.resolveDrawable(skin, it) }
            val imageButtonStyle = ImageButton.ImageButtonStyle(up, null, null, null, null, null)
            val visImageButtonStyle = VisImageButton.VisImageButtonStyle(up, null, null, null, null, null)
            skin.add(name, imageButtonStyle)
            skin.add(name, visImageButtonStyle)
        }

        buttons.forEach { (name, definition) ->
            val up = definition.up?.let { skinResolvers.resolveDrawable(skin, it) }
            val over = definition.over?.let { skinResolvers.resolveDrawable(skin, it) }
            skin.add(name, Button.ButtonStyle(up, null, null).apply {
                this.over = over
            })
        }

        labels.forEach { (name, definition) ->
            skin.add(
                name,
                Label.LabelStyle(
                    skinResolvers.resolveFont(skin, definition.font),
                    definition.fontColor?.let { skinResolvers.resolveColor(skin, it) }
                )
            )
        }

        textFields.forEach { (name, definition) ->
            val font = skinResolvers.resolveFont(skin, definition.font)
            val fontColor = definition.fontColor?.let { skinResolvers.resolveColor(skin, it) } ?: Color.WHITE
            skin.add(name, TextField.TextFieldStyle(font, fontColor, null, null, null))
            skin.add(name, VisTextField.VisTextFieldStyle(font, fontColor, null, null, null))
        }

        progressBars.forEach { (name, definition) ->
            skin.add(
                name,
                ProgressBar.ProgressBarStyle(null, null).apply {
                    knobBefore = definition.knobBefore?.let { skinResolvers.resolveDrawable(skin, it) }
                }
            )
        }
    }
}

typealias DrawableThemeDefinition = String
typealias FontThemeDefinition = String
typealias ColorThemeDefinition = String

@Serializable
data class ImageButtonThemeDefinition(
    val up: DrawableThemeDefinition?
)

@Serializable
data class ButtonThemeDefinition(
    val up: DrawableThemeDefinition?,
    val over: DrawableThemeDefinition?
)

@Serializable
data class LabelThemeDefinition(
    val font: FontThemeDefinition,
    val fontColor: ColorThemeDefinition?
)

@Serializable
data class TextFieldThemeDefinition(
    val font: FontThemeDefinition,
    val fontColor: ColorThemeDefinition?
)

@Serializable
data class ProgressBarThemeDefinition(
    val knobBefore: DrawableThemeDefinition?
)

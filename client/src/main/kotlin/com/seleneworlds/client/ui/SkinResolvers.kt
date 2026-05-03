package com.seleneworlds.client.ui

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.seleneworlds.client.bundles.BundleFileResolver

class SkinResolvers(private val bundleFileResolver: BundleFileResolver) {

    fun resolveFont(skin: Skin, fontName: String): BitmapFont {
        return skin.optional(fontName, BitmapFont::class.java)
    }

    fun resolveDrawable(skin: Skin?, path: String): Drawable? {
        return skin?.optional(path, TextureRegion::class.java)?.let {
            return TextureRegionDrawable(it)
        }
    }

    fun resolveDrawable(theme: ThemeApi?, path: String): Drawable? {
        return resolveDrawable(theme?.skin, path)
    }

    fun resolveColor(skin: Skin, colorString: String): Color {
        return when {
            colorString.startsWith("#") -> {
                try {
                    val hex = colorString.substring(1)
                    when (hex.length) {
                        6 -> Color.valueOf(hex + "FF")
                        8 -> Color.valueOf(hex)
                        else -> throw IllegalArgumentException("Invalid hex color: $colorString")
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid hex color: $colorString", e)
                }
            }

            else -> {
                skin.optional(colorString, Color::class.java)
                    ?: throw IllegalArgumentException("Color not found in skin: $colorString")
            }
        }
    }

    fun resolveFile(path: String): FileHandle {
        return bundleFileResolver.resolve(path)
    }
}

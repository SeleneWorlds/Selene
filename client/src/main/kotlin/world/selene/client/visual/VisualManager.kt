package world.selene.client.visual

import world.selene.client.data.VisualRegistry
import world.selene.client.data.AnimatedVisualDefinition
import world.selene.client.data.SimpleVisualDefinition
import world.selene.client.data.VariantsVisualDefinition
import world.selene.client.rendering.AnimatedDrawableOptions
import world.selene.client.rendering.DrawableManager
import world.selene.client.rendering.DrawableOptions
import world.selene.client.rendering.Visual

class VisualManager(private val drawableManager: DrawableManager, private val visualRegistry: VisualRegistry) {

    fun createVisual(name: String, context: VisualCreationContext): Visual? {
        val visualDef = visualRegistry.get(name) ?: return null
        return when (visualDef) {
            is SimpleVisualDefinition -> {
                val drawable = drawableManager.getDrawable(
                    visualDef.texture, DrawableOptions(
                        offsetX = visualDef.offsetX,
                        offsetY = visualDef.offsetY,
                        flipX = visualDef.flipX,
                        flipY = visualDef.flipY
                    )
                ) ?: return null
                DrawableIsoVisual(drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY)
            }

            is VariantsVisualDefinition -> {
                val options = DrawableOptions(
                    offsetX = visualDef.offsetX,
                    offsetY = visualDef.offsetY,
                    flipX = visualDef.flipX,
                    flipY = visualDef.flipY
                )
                val variant = visualDef.textures[context.coordinate.x % visualDef.textures.size]
                val drawable = drawableManager.getDrawable(variant, options) ?: return null
                DrawableIsoVisual(drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY)
            }

            is AnimatedVisualDefinition -> {
                val frameOptions = DrawableOptions(
                    offsetX = visualDef.offsetX,
                    offsetY = visualDef.offsetY,
                    flipX = visualDef.flipX,
                    flipY = visualDef.flipY
                )
                val frames = visualDef.textures.map { it to frameOptions }
                val options = AnimatedDrawableOptions(
                    duration = visualDef.duration
                )
                val managedKey = if (!visualDef.instance) name else null
                val drawable = drawableManager.getAnimatedDrawable(frames, options, managedKey)
                DrawableIsoVisual(drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY).apply {
                    if (managedKey != null) {
                        shouldUpdate = false
                    }
                }
            }

            else -> null
        }
    }
}

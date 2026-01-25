package world.selene.client.rendering.visual

import world.selene.client.rendering.animator.DrawableAnimator
import world.selene.client.rendering.drawable.AnimatedDrawableOptions
import world.selene.client.rendering.drawable.DrawableManager
import world.selene.client.rendering.drawable.DrawableOptions
import world.selene.client.rendering.drawable.TextDrawableOptions
import world.selene.client.rendering.visual2d.DrawableVisual2D
import world.selene.client.rendering.visual2d.iso.DrawableIsoVisual
import world.selene.client.rendering.visual2d.iso.DynamicDrawableIsoVisual

class VisualFactory(private val drawableManager: DrawableManager) {

    fun createVisual(visualDef: VisualDefinition, context: VisualCreationContext): Visual? {
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
                DrawableIsoVisual(visualDef, drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY)
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
                DrawableIsoVisual(visualDef, drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY)
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
                val sharedIdentifier = if (!visualDef.instanced) visualDef.identifier else null
                val drawable = drawableManager.getAnimatedDrawable(frames, options, sharedIdentifier)
                DrawableIsoVisual(visualDef, drawable, visualDef.sortLayerOffset, visualDef.surfaceOffsetY).apply {
                    if (sharedIdentifier != null) {
                        shouldUpdate = false
                    }
                }
            }

            is AnimatorVisualDefinition -> {
                val animationController = context.animatorController ?: return null
                val drawableAnimator = DrawableAnimator(animationController)
                visualDef.animations.forEach { (animationName, frames) ->
                    val options = AnimatedDrawableOptions(
                        duration = frames.duration ?: (1f / 30f)
                    )
                    val drawableFrames = frames.textures.map {
                        it to DrawableOptions(
                            offsetX = frames.offsetX ?: visualDef.offsetX,
                            offsetY = frames.offsetY ?: visualDef.offsetY,
                            flipX = frames.flipX,
                            flipY = frames.flipY
                        )
                    }
                    val drawable = drawableManager.getAnimatedDrawable(drawableFrames, options)
                    drawableAnimator.addAnimation(animationName, drawable)
                }
                DynamicDrawableIsoVisual(
                    visualDef,
                    drawableAnimator::drawable,
                    visualDef.sortLayerOffset,
                    visualDef.surfaceOffsetY
                )
            }

            is TextVisualDefinition -> {
                val text = context.overrides["text"] as String? ?: visualDef.text
                val options = TextDrawableOptions(horizontalAlign = visualDef.align.align)
                val drawable = drawableManager.getTextDrawable(text, options)
                DrawableVisual2D(visualDef, drawable)
            }

            else -> null
        }
    }
}

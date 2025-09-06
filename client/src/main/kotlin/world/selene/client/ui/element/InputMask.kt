package world.selene.client.ui.element

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.impl.tag.AbstractGroupLmlTag
import com.github.czyzby.lml.parser.tag.LmlActorBuilder
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.parser.tag.LmlTagProvider

class InputMask() : WidgetGroup() {

    var mask: Drawable? = null
        set(value) {
            if (field != value) {
                field = value
                disposeCachedPixmap()
                invalidate()
            }
        }

    var actor: Actor? = null
        set(value) {
            if (value == this) throw IllegalArgumentException("actor cannot be the InputMask.")
            if (field == value) return
            if (field != null) super.removeActor(field)
            field = value
            if (value != null) super.addActor(value)
            invalidate()
        }

    private var sizeInvalid = true
    private var cachedPixmap: Pixmap? = null
    private var cachedTextureData: TextureData? = null

    init {
        touchable = Touchable.childrenOnly
        isTransform = false
    }

    override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        // If no mask is set, behave like normal
        val maskDrawable = mask ?: return super.hit(x, y, touchable)

        // If we are outside the mask, do not hit the child
        if (!isWithinMask(x, y, maskDrawable)) {
            return null
        }

        return super.hit(x, y, touchable)
    }

    private fun isWithinMask(x: Float, y: Float, maskDrawable: Drawable): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return false
        }

        // Only TextureRegionDrawable gives us access to texture data
        if (maskDrawable is TextureRegionDrawable) {
            val region = maskDrawable.region
            val texture = region.texture

            // Convert local coordinates to texture coordinates
            val textureX = (x / width * region.regionWidth + region.regionX).toInt()
            val textureY = (y / height * region.regionHeight + region.regionY).toInt()

            // Check if coordinates are within texture bounds
            if (textureX < region.regionX || textureX >= region.regionX + region.regionWidth ||
                textureY < region.regionY || textureY >= region.regionY + region.regionHeight
            ) {
                return false
            }

            // Get or create cached pixmap
            val pixmap = cachedPixmap ?: run {
                val textureData = texture.textureData
                if (!textureData.isPrepared) {
                    textureData.prepare()
                }

                val newPixmap = textureData.consumePixmap()
                cachedPixmap = newPixmap
                cachedTextureData = textureData
                newPixmap
            }

            try {
                // LibGDX pixmap coordinates are flipped vertically
                val pixmapY = pixmap.height - 1 - textureY
                val pixel = pixmap.getPixel(textureX, pixmapY)

                // Extract alpha from RGBA8888 format
                val alpha = pixel and 0xFF
                return alpha > 0
            } catch (_: Exception) {
                return true
            }
        } else {
            throw IllegalArgumentException("InputMask only supports TextureRegionDrawable")
        }
    }

    private fun disposeCachedPixmap() {
        cachedPixmap?.let { pixmap ->
            cachedTextureData?.let { textureData ->
                if (textureData.disposePixmap()) {
                    pixmap.dispose()
                }
            }
        }
        cachedPixmap = null
        cachedTextureData = null
    }

    override fun invalidate() {
        super.invalidate()
        sizeInvalid = true
    }

    private fun computeSize() {
        sizeInvalid = false
        val child = this.actor
        if (child != null) {
            child.setBounds(0f, 0f, width, height)
            if (child is Layout) {
                child.validate()
            }
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
    }

    override fun layout() {
        if (sizeInvalid) computeSize()

        val child = this.actor
        if (child != null) {
            child.setBounds(0f, 0f, width, height)
            if (child is Layout) {
                child.validate()
            }
        }
    }

    override fun getPrefWidth(): Float {
        if (sizeInvalid) computeSize()
        val child = this.actor
        return when {
            child is Layout -> child.prefWidth
            child != null -> child.width
            else -> 0f
        }
    }

    override fun getPrefHeight(): Float {
        if (sizeInvalid) computeSize()
        val child = this.actor
        return when {
            child is Layout -> child.prefHeight
            child != null -> child.height
            else -> 0f
        }
    }

    override fun getMinWidth(): Float {
        val child = this.actor
        return when {
            child is Layout -> child.minWidth
            child != null -> child.width
            else -> 0f
        }
    }

    override fun getMinHeight(): Float {
        val child = this.actor
        return when {
            child is Layout -> child.minHeight
            child != null -> child.height
            else -> 0f
        }
    }

    override fun getMaxWidth(): Float {
        val child = this.actor
        return when {
            child is Layout -> child.maxWidth
            child != null -> child.width
            else -> 0f
        }
    }

    override fun getMaxHeight(): Float {
        val child = this.actor
        return when {
            child is Layout -> child.maxHeight
            child != null -> child.height
            else -> 0f
        }
    }

    override fun addActor(actor: Actor?) {
        if (this.actor != null) {
            throw IllegalArgumentException("InputMask may only have one child.")
        }
        this.actor = actor
    }

    override fun addActorAt(index: Int, actor: Actor?) {
        throw UnsupportedOperationException("Use InputMask#setActor.")
    }

    override fun addActorBefore(actorBefore: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use InputMask#setActor.")
    }

    override fun addActorAfter(actorAfter: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use InputMask#setActor.")
    }

    override fun removeActor(actor: Actor): Boolean {
        if (actor !== this.actor) return false
        this.actor = null
        return true
    }

    override fun removeActor(actor: Actor, unfocus: Boolean): Boolean {
        if (actor !== this.actor) return false
        this.actor = null
        return super.removeActor(actor, unfocus)
    }

    override fun removeActorAt(index: Int, unfocus: Boolean): Actor? {
        val actor = super.removeActorAt(index, unfocus)
        if (actor === this.actor) this.actor = null
        return actor
    }

    override fun remove(): Boolean {
        // Clean up cached pixmap when removing from scene
        disposeCachedPixmap()
        return super.remove()
    }

    class InputMaskLmlTag(parser: LmlParser, parentTag: LmlTag?, rawTagData: StringBuilder) :
        AbstractGroupLmlTag(parser, parentTag, rawTagData) {
        override fun getNewInstanceOfGroup(builder: LmlActorBuilder): Group {
            return InputMask()
        }
    }

    object InputMaskLmlTagProvider : LmlTagProvider {
        override fun create(
            parser: LmlParser,
            parentTag: LmlTag?,
            rawTagData: StringBuilder
        ): LmlTag {
            return InputMaskLmlTag(parser, parentTag, rawTagData)
        }
    }

}

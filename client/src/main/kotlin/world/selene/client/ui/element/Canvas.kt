package world.selene.client.ui.element

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.impl.tag.AbstractGroupLmlTag
import com.github.czyzby.lml.parser.tag.LmlActorBuilder
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.parser.tag.LmlTagProvider
import kotlin.math.max
import kotlin.math.min


class Canvas() : WidgetGroup() {

    var canvasPrefWidth = 0f
    var canvasPrefHeight = 0f
    var canvasMinWidth = 0f
    var canvasMinHeight = 0f
    var canvasMaxWidth = 0f
    var canvasMaxHeight = 0f
    var sizeInvalid = true

    init {
        isTransform = false
        touchable = Touchable.childrenOnly
    }

    override fun invalidate() {
        super.invalidate()
        sizeInvalid = true
    }

    private fun computeSize() {
        sizeInvalid = false
        canvasPrefWidth = 0f
        canvasPrefHeight = 0f
        canvasMinWidth = 0f
        canvasMinHeight = 0f
        canvasMaxWidth = 0f
        canvasMaxHeight = 0f
        for (child in children) {
            var childMaxWidth: Float
            var childMaxHeight: Float
            if (child is Layout) {
                canvasPrefWidth = max(canvasPrefWidth, child.prefWidth)
                canvasPrefHeight = max(canvasPrefHeight, child.prefHeight)
                canvasMinWidth = max(canvasMinWidth, child.minWidth)
                canvasMinHeight = max(canvasMinHeight, child.minHeight)
                childMaxWidth = child.maxWidth
                childMaxHeight = child.maxHeight
            } else {
                canvasPrefWidth = max(canvasPrefWidth, child.getWidth())
                canvasPrefHeight = max(canvasPrefHeight, child.getHeight())
                canvasMinWidth = max(canvasMinWidth, child.getWidth())
                canvasMinHeight = max(canvasMinHeight, child.getHeight())
                childMaxWidth = 0f
                childMaxHeight = 0f
            }
            if (childMaxWidth > 0) canvasMaxWidth =
                if (canvasMaxWidth == 0f) childMaxWidth else min(canvasMaxWidth, childMaxWidth)
            if (childMaxHeight > 0) canvasMaxHeight =
                if (canvasMaxHeight == 0f) childMaxHeight else min(canvasMaxHeight, childMaxHeight)
        }
    }

    fun add(actor: Actor) {
        addActor(actor)
    }

    override fun layout() {
        if (sizeInvalid) computeSize()
        val children = getChildren()
        for (child in children.reversed()) {
            if (child is Layout) (child as Layout).validate()
        }
    }

    override fun getPrefWidth(): Float {
        if (sizeInvalid) computeSize()
        return canvasPrefWidth
    }

    override fun getPrefHeight(): Float {
        if (sizeInvalid) computeSize()
        return canvasPrefHeight
    }

    override fun getMinWidth(): Float {
        if (sizeInvalid) computeSize()
        return canvasMinWidth
    }

    override fun getMinHeight(): Float {
        if (sizeInvalid) computeSize()
        return canvasMinHeight
    }

    override fun getMaxWidth(): Float {
        if (sizeInvalid) computeSize()
        return canvasMaxWidth
    }

    override fun getMaxHeight(): Float {
        if (sizeInvalid) computeSize()
        return canvasMaxHeight
    }

    class CanvasLmlTag(parser: LmlParser, parentTag: LmlTag?, rawTagData: StringBuilder) :
        AbstractGroupLmlTag(parser, parentTag, rawTagData) {
        override fun getNewInstanceOfGroup(builder: LmlActorBuilder): Group {
            return Canvas()
        }
    }

    object CanvasLmlTagProvider : LmlTagProvider {
        override fun create(
            parser: LmlParser,
            parentTag: LmlTag?,
            rawTagData: StringBuilder
        ): LmlTag {
            return CanvasLmlTag(parser, parentTag, rawTagData)
        }
    }

}


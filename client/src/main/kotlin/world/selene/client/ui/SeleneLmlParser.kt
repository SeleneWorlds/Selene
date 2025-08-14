package world.selene.client.ui

import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.impl.tag.actor.provider.CheckBoxLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.DialogLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ImageButtonLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ImageLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ImageTextButtonLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.LabelLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ListLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ProgressBarLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.ScrollPaneLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.SelectBoxLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.SliderLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.SplitPaneLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TableLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TextAreaLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TextButtonLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TextFieldLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TreeLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.WindowLmlTagProvider
import com.github.czyzby.lml.parser.tag.LmlAttribute
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.util.LmlUtilities
import com.github.czyzby.lml.vis.util.VisLml
import com.github.czyzby.lml.vis.util.VisLmlParserBuilder
import world.selene.client.ui.element.Canvas


object SeleneLmlParser {
    fun parser(): VisLmlParserBuilder {
        return VisLml.parser().apply {
            // VisLabel forces the VisUI skin and provides no other benefits. It makes no sense to be applied to label.
            // Therefore, we revert "label" to use the default provider instead of the visui-lml one.
            tag(LabelLmlTagProvider(), "label")
            tag(TextFieldLmlTagProvider(), "textField")
            tag(ScrollPaneLmlTagProvider(), "scrollPane")
            tag(CheckBoxLmlTagProvider(), "checkBox")
            tag(DialogLmlTagProvider(), "dialog")
            tag(ImageButtonLmlTagProvider(), "imageButton")
            tag(ImageLmlTagProvider(), "image")
            tag(ImageTextButtonLmlTagProvider(), "imageTextButton")
            tag(LabelLmlTagProvider(), "label")
            tag(ListLmlTagProvider(), "list")
            tag(ProgressBarLmlTagProvider(), "progressBar")
            tag(ScrollPaneLmlTagProvider(), "scrollPane")
            tag(SelectBoxLmlTagProvider(), "selectBox")
            tag(SliderLmlTagProvider(), "slider")
            tag(SplitPaneLmlTagProvider(), "splitPane")
            tag(TableLmlTagProvider(), "table")
            tag(TextAreaLmlTagProvider(), "textArea")
            tag(TextButtonLmlTagProvider(), "textButton")
            tag(TextFieldLmlTagProvider(), "textField")
            tag(TreeLmlTagProvider(), "tree")
            tag(WindowLmlTagProvider(), "window")

            tag(Canvas.CanvasLmlTagProvider, "canvas")

            // For some reason these attributes are not supported by default, so we expose them to LML
            attribute(object : LmlAttribute<TextField> {
                override fun getHandledType(): Class<TextField> {
                    return TextField::class.java
                }

                override fun process(
                    parser: LmlParser,
                    tag: LmlTag,
                    actor: TextField,
                    rawAttributeData: String
                ) {
                    actor.maxLength = rawAttributeData.toInt()
                }

            }, "maxLength")

            attribute(object : LmlAttribute<TextField> {
                override fun getHandledType(): Class<TextField> {
                    return TextField::class.java
                }

                override fun process(
                    parser: LmlParser,
                    tag: LmlTag,
                    actor: TextField,
                    rawAttributeData: String
                ) {
                    actor.alignment = LmlUtilities.parseAlignment(parser, actor, rawAttributeData)
                }

            }, "alignment", "align")

            attribute(object : LmlAttribute<VerticalGroup> {
                override fun getHandledType(): Class<VerticalGroup> {
                    return VerticalGroup::class.java
                }

                override fun process(
                    parser: LmlParser,
                    tag: LmlTag,
                    actor: VerticalGroup,
                    rawAttributeData: String
                ) {
                    actor.columnAlign(LmlUtilities.parseAlignment(parser, actor, rawAttributeData))
                }

            }, "columnAlign")

            // Width and Height in Containers seems to only be set if a layout does it for them?
            // Not sure if this is the solution or if Canvas should just set items to their pref size
            attribute(object : LmlAttribute<Container<*>> {
                override fun getHandledType(): Class<Container<*>> {
                    return Container::class.java
                }

                override fun process(
                    parser: LmlParser,
                    tag: LmlTag,
                    actor: Container<*>,
                    rawAttributeData: String
                ) {
                    val width = rawAttributeData.toFloat()
                    actor.width = width
                    actor.width(width)
                }

            }, "width")

            attribute(object : LmlAttribute<Container<*>> {
                override fun getHandledType(): Class<Container<*>> {
                    return Container::class.java
                }

                override fun process(
                    parser: LmlParser,
                    tag: LmlTag,
                    actor: Container<*>,
                    rawAttributeData: String
                ) {
                    val height = rawAttributeData.toFloat()
                    actor.height = height
                    actor.height(height)
                }

            }, "height")
        }
    }
}

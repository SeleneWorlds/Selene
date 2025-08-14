package world.selene.client.ui

import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.impl.tag.actor.provider.LabelLmlTagProvider
import com.github.czyzby.lml.parser.impl.tag.actor.provider.TextFieldLmlTagProvider
import com.github.czyzby.lml.parser.tag.LmlAttribute
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.util.LmlUtilities
import com.github.czyzby.lml.vis.util.VisLml
import com.github.czyzby.lml.vis.util.VisLmlParserBuilder

object SeleneLmlParser {
    fun parser(): VisLmlParserBuilder {
        return VisLml.parser().apply {
            // VisLabel forces the VisUI skin and provides no other benefits. It makes no sense to be applied to label.
            // Therefore, we revert "label" to use the default provider instead of the visui-lml one.
            tag(LabelLmlTagProvider(), "label")
            tag(TextFieldLmlTagProvider(), "textField")

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

            }, "alignment")
        }
    }
}

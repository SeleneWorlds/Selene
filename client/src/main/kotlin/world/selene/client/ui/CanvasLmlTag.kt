package world.selene.client.ui

import com.badlogic.gdx.scenes.scene2d.Group
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.impl.tag.AbstractGroupLmlTag
import com.github.czyzby.lml.parser.tag.LmlActorBuilder
import com.github.czyzby.lml.parser.tag.LmlTag
import java.lang.StringBuilder

class CanvasLmlTag(parser: LmlParser, parentTag: LmlTag?, rawTagData: StringBuilder) :
    AbstractGroupLmlTag(parser, parentTag, rawTagData) {
    override fun getNewInstanceOfGroup(builder: LmlActorBuilder): Group {
        return Canvas()
    }
}
package world.selene.client.ui

import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.parser.tag.LmlTagProvider


class CanvasLmlTagProvider : LmlTagProvider {
    override fun create(
        parser: LmlParser,
        parentTag: LmlTag?,
        rawTagData: StringBuilder
    ): LmlTag {
        return CanvasLmlTag(parser, parentTag, rawTagData)
    }

}

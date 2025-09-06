package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*

fun JsonNode?.asAny(): Any? {
    return when (this) {
        is LongNode -> asLong()
        is IntNode, is ShortNode -> asInt()
        is FloatNode, is DoubleNode -> asDouble()
        is BooleanNode -> asBoolean()
        is TextNode -> asText()
        else -> null
    }
}

package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode

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

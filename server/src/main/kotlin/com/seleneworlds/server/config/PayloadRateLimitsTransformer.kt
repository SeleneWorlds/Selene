package com.seleneworlds.server.config

import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.denormalize
import com.sksamuel.hoplite.transformer.NodeTransformer

/** Flattens dotted payload paths into colon-separated IDs before config decoding. */
object PayloadRateLimitsTransformer : NodeTransformer {
    override fun transformPathElement(element: String): String = element

    override fun transform(node: Node, sealedTypeDiscriminatorField: String?): Node {
        if (node !is MapNode) return node
        val key = node.map.keys.firstOrNull { it.replace("_", "").lowercase() == "payloadratelimits" }
            ?: return node
        val limits = node.map[key] as? MapNode ?: return node
        val flattened = linkedMapOf<String, Node>()

        fun collect(branch: MapNode, prefix: String) {
            for ((segment, child) in branch.denormalize().map) {
                val id = if (prefix.isEmpty()) segment else "$prefix:$segment"
                if (child is MapNode && child.map.isNotEmpty() && child.map.values.all { it is MapNode }) {
                    collect(child, id)
                } else {
                    require(id !in flattened) { "Multiple rate limits configured for payload $id" }
                    flattened[id] = if (child is MapNode) {
                        child.copy(sourceKey = "${limits.sourceKey}.$id")
                    } else child
                }
            }
        }

        collect(limits, "")
        return node.copy(map = node.map + (key to limits.copy(map = flattened)))
    }
}

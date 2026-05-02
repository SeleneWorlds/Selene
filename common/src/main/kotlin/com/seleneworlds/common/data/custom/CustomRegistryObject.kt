package com.seleneworlds.common.data.custom

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import com.seleneworlds.common.util.asAny
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.RegistryObject
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference

class CustomRegistryObject(val customRegistry: CustomRegistry, override val identifier: Identifier, val element: JsonNode) :
    IdResolvable<Identifier, CustomRegistryObject>,
    RegistryObject<CustomRegistryObject>(),
    MetadataHolder {
    val resolvableReference = ResolvableReference(CustomRegistryObject::class, identifier, customRegistry, this)

    override val registry: Registry<CustomRegistryObject>
        get() = customRegistry

    override var id: Int = -1

    override val metadata: Map<String, Any> by lazy {
        (element.get("metadata") as? ObjectNode)?.let { metadataNode ->
            val metadataMap = mutableMapOf<String, Any>()
            metadataNode.forEachEntry { key, node ->
                val value = node.asAny()
                if (value != null) {
                    metadataMap[key] = value
                }
            }
            metadataMap
        } ?: emptyMap()
    }

    fun getMetadata(key: String): Any? {
        return element["metadata"]?.get(key)?.asAny()
    }

    override fun resolvableReference(): ResolvableReference<Identifier, CustomRegistryObject> {
        return resolvableReference
    }

    override fun toString(): String {
        return "CustomRegistryObject(${registry.name}, $identifier, $element)"
    }

}

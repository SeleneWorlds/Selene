package world.selene.common.data.custom

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import world.selene.common.util.asAny
import world.selene.common.lua.*
import world.selene.common.data.Identifier
import world.selene.common.data.RegistryObject
import world.selene.common.data.MetadataHolder
import world.selene.common.data.Registry
import world.selene.common.util.ResolvableReference

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

package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.RegistryObject
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.unwrap
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference

class CustomRegistryObject(val customRegistry: CustomRegistry, override val identifier: Identifier, val element: JsonElement) :
    IdResolvable<Identifier, CustomRegistryObject>,
    RegistryObject<CustomRegistryObject>(),
    MetadataHolder {
    val resolvableReference = ResolvableReference(CustomRegistryObject::class, identifier, customRegistry, this)

    override val registry: Registry<CustomRegistryObject>
        get() = customRegistry

    override var id: Int = -1

    override val metadata: SerializedMap by lazy {
        (element as? JsonObject)?.get("metadata")?.let { metadataNode ->
            val metadataMap = mutableMapOf<String, Any?>()
            (metadataNode as? JsonObject)?.forEach { (key, node) ->
                metadataMap[key] = node.unwrap()
            }
            metadataMap
        } ?: emptyMap()
    }

    fun getMetadata(key: String): Any? {
        return (element as? JsonObject)?.get("metadata")?.let { it as? JsonObject }?.get(key)?.unwrap()
    }

    override fun resolvableReference(): ResolvableReference<Identifier, CustomRegistryObject> {
        return resolvableReference
    }

    override fun toString(): String {
        return "CustomRegistryObject(${registry.name}, $identifier, $element)"
    }

}

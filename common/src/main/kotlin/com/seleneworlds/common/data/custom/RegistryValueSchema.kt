package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.*

/** Check substituted data using declarative registry schemas; script-provided schemas remain script-owned. */
internal class RegistryValueSchema(private val namedSchema: (String) -> JsonElement?) {
    fun validate(value: JsonElement, definition: JsonElement, path: String = "", depth: Int = 0) {
        require(depth < 128) { "Schema nesting exceeds 128 at $path" }
        val schema = definition as? JsonObject
        val kind = (definition as? JsonPrimitive)?.content ?: schema?.get("type")?.jsonPrimitive?.content ?: return
        fun check(valid: Boolean) = require(valid) { "Expected $kind at ${path.ifEmpty { "/" }}" }
        fun child(value: JsonElement, definition: JsonElement, key: String) = validate(value, definition, "$path/$key", depth + 1)
        if (':' in kind) {
            val named = requireNotNull(namedSchema(kind)) { "Unknown schema: $kind" }
            child(value, bind(named, schema?.get("parameters") as? JsonObject ?: JsonObject(emptyMap())), "<$kind>")
            return
        }
        when (kind) {
            "coordinate" -> check(value is JsonObject && listOf("x", "y", "z").all { integer(value[it]) })
            "integer" -> check(integer(value))
            "number", "distance" -> check(value is JsonPrimitive && !value.isString && value.doubleOrNull?.isFinite() == true)
            "boolean", "enabled" -> check(value is JsonPrimitive && !value.isString && value.booleanOrNull != null)
            "string", "script", "registry", "Direction", "message" -> check(value is JsonPrimitive && value.isString)
            "array" -> {
                check(value is JsonArray)
                schema?.get("valueType")?.let { type -> (value as JsonArray).forEachIndexed { index, item -> child(item, type, "$index") } }
            }
            "object", "map", "scripted_schema_lookup" -> {
                check(value is JsonObject)
                val objectValue = value as JsonObject
                if (kind == "map") schema?.get("valueType")?.let { type -> objectValue.forEach { (key, item) -> child(item, type, key) } }
                (schema?.get("properties") as? JsonObject)?.forEach { (key, type) ->
                    val item = objectValue[key]
                    if (item != null) child(item, type, key)
                    else require((type as? JsonObject)?.get("optional") == JsonPrimitive(true)) { "Missing required value at $path/$key" }
                }
            }
            "enum" -> check((schema?.get("values") as? JsonArray)?.any { (it as? JsonObject)?.get("value") == value } == true)
            "range" -> check(value is JsonObject && listOf("min", "max").all { (value[it] as? JsonPrimitive)?.doubleOrNull?.isFinite() == true })
            "area" -> {
                check(value is JsonObject)
                for (key in listOf("include", "exclude")) {
                    val shapes = (value as JsonObject)[key]
                    check(shapes is JsonArray)
                    (shapes as JsonArray).forEachIndexed { index, shape ->
                        check(shape is JsonObject)
                        child(shape, JsonPrimitive("coordinate"), "$key/$index")
                        val type = (shape as JsonObject)["type"]
                        check(type == JsonPrimitive("rectangle") || type == JsonPrimitive("circle"))
                        val dimensions = if (type == JsonPrimitive("rectangle")) listOf("width", "height") else listOf("radius")
                        check(dimensions.all { integer(shape[it]) && shape[it]!!.jsonPrimitive.long >= if (it == "radius") 0 else 1 })
                    }
                }
            }
            // These types select their contracts at runtime, outside the declarative schema layer.
            "any", "schema_lookup", "registry_field_reference" -> Unit
        }
    }

    private fun integer(value: JsonElement?) = value is JsonPrimitive && !value.isString && value.intOrNull != null

    private fun bind(template: JsonElement, parameters: JsonObject): JsonElement {
        if (template is JsonArray) return JsonArray(template.map { bind(it, parameters) })
        if (template !is JsonObject) return template
        val parameter = template["\$parameter"]?.jsonPrimitive?.content
        if (parameter != null) {
            val value = requireNotNull(parameters[parameter] ?: template["default"]) { "Missing schema parameter: $parameter" }
            if (template["transform"] == JsonPrimitive("enum")) {
                require(value is JsonObject) { "Enum schema parameter must be an object: $parameter" }
                return buildJsonObject {
                    put("type", "enum")
                    put("values", JsonArray(value.keys.sorted().map { buildJsonObject { put("value", it); put("label", it) } }))
                }
            }
            return value
        }
        return JsonObject(template.mapValues { bind(it.value, parameters) })
    }
}

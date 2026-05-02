package com.seleneworlds.common.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.inputStream

@OptIn(ExperimentalSerializationApi::class)
val seleneJson = Json {
    ignoreUnknownKeys = true
    decodeEnumsCaseInsensitive = true
    explicitNulls = false
}

typealias SerializedMap = Map<String, Any?>

object SerializedMapSerializer : KSerializer<SerializedMap> {
    private val delegate = JsonElement.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: SerializedMap) {
        delegate.serialize(encoder, value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): SerializedMap {
        return delegate.deserialize(decoder).toSerializedMap()
    }
}
object NullableSerializedMapSerializer : KSerializer<SerializedMap?> by SerializedMapSerializer.nullable

fun SerializedMap.toJsonElement(): JsonObject = buildJsonObject {
    for ((key, value) in this@toJsonElement) {
        put(key, value.toJsonElement())
    }
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Short -> JsonPrimitive(this)
    is Byte -> JsonPrimitive(this)
    is Float -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> buildJsonObject {
        for ((key, value) in this@toJsonElement) {
            val stringKey = key as? String
                ?: throw SerializationException("Unsupported JSON object key type: ${key?.let { it::class.qualifiedName } ?: "null"}")
            put(stringKey, value.toJsonElement())
        }
    }

    is Iterable<*> -> buildJsonArray {
        for (entry in this@toJsonElement) {
            add(entry.toJsonElement())
        }
    }

    is Array<*> -> buildJsonArray {
        for (entry in this@toJsonElement) {
            add(entry.toJsonElement())
        }
    }

    else -> throw SerializationException("Unsupported JSON value type: ${this::class.qualifiedName}")
}

fun JsonElement.unwrap(): Any? = when (this) {
    JsonNull -> null
    is JsonObject -> entries.associate { (key, value) ->
        key to value.unwrap()
    }

    is JsonArray -> map { it.unwrap() }
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull!!
        intOrNull != null -> intOrNull!!
        longOrNull != null -> longOrNull!!
        doubleOrNull != null -> doubleOrNull!!
        else -> content
    }
}

fun JsonElement.toSerializedMap(): SerializedMap {
    return when (this) {
        is JsonObject -> entries.associate { (key, value) ->
            key to value.unwrap()
        }

        else -> throw SerializationException("Expected JSON object for serialized map")
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> Json.decodeFromFile(
    serializer: DeserializationStrategy<T>,
    file: File
): T {
    return file.inputStream().use {
        decodeFromStream(serializer, it)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> Json.decodeFromFile(
    serializer: DeserializationStrategy<T>,
    file: Path
): T {
    return file.inputStream().use {
        decodeFromStream(serializer, it)
    }
}

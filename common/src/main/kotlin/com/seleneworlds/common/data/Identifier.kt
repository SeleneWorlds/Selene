package com.seleneworlds.common.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Identifier.Companion.Serializer::class)
data class Identifier(val namespace: String, val path: String) : Comparable<Identifier> {

    companion object {
        const val DEFAULT_NAMESPACE = "selene"

        private val COMPARATOR = Comparator.comparing<Identifier, String> { it.namespace }.then(Comparator.comparing { it.path })

        fun withDefaultNamespace(path: String): Identifier {
            return Identifier(DEFAULT_NAMESPACE, path)
        }

        fun parse(input: String): Identifier {
            val colonIndex = input.indexOf(':')
            return if (colonIndex != -1) {
                Identifier(input.substring(0, colonIndex), input.substring(colonIndex + 1))
            } else {
                withDefaultNamespace(input)
            }
        }

        object Serializer : KSerializer<Identifier> {
            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Identifier) {
                encoder.encodeString(value.toString())
            }

            override fun deserialize(decoder: Decoder): Identifier {
                return parse(decoder.decodeString())
            }
        }
    }

    init {
        require(namespace.isNotEmpty()) { "Namespace cannot be empty" }
        require(path.isNotEmpty()) { "Path cannot be empty" }
    }

    override fun toString(): String = "$namespace:$path"

    fun replacePath(newPath: String): Identifier = Identifier(namespace, newPath)

    fun replaceNamespace(newNamespace: String): Identifier = Identifier(newNamespace, path)

    fun withPrefix(prefix: String): Identifier = Identifier(namespace, prefix + path)

    fun withSuffix(suffix: String): Identifier = Identifier(namespace, path + suffix)

    override fun compareTo(other: Identifier): Int {
        return COMPARATOR.compare(this, other)
    }
}

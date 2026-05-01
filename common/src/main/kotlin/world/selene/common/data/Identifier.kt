package world.selene.common.data

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
@JsonSerialize(using = Identifier.Companion.Serializer::class)
@JsonDeserialize(using = Identifier.Companion.Deserializer::class)
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

        /**
         * Jackson serializer for Identifier - converts to string format "namespace:path"
         */
        class Serializer : JsonSerializer<Identifier>() {
            override fun serialize(value: Identifier, gen: JsonGenerator, serializers: SerializerProvider) {
                gen.writeString(value.toString())
            }
        }

        /**
         * Jackson deserializer for Identifier - parses string format "namespace:path"
         */
        class Deserializer : JsonDeserializer<Identifier>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Identifier {
                val value = p.valueAsString ?: throw ctxt.weirdStringException(
                    null,
                    Identifier::class.java,
                    "Identifier cannot be null"
                )
                return parse(value)
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

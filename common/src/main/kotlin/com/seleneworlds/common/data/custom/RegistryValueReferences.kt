package com.seleneworlds.common.data.custom

import com.seleneworlds.common.data.Identifier
import kotlinx.serialization.json.*

/** Value substitution for custom registry JSON, independent of any content-specific concepts. */
class RegistryValueReferences(private val registries: () -> Map<Identifier, CustomRegistry>) {
    data class Entry(val registry: Identifier, val entry: Identifier)
    private data class Target(val entry: Entry, val path: String) {
        override fun toString(): String = "${entry.registry}/${entry.entry}#$path"
    }

    fun resolveField(owner: CustomRegistryObject, key: String): JsonElement? {
        val value = (owner.element as? JsonObject)?.get(key) ?: return null
        if (!containsReference(value)) return value
        val registry = registries().entries.firstOrNull { it.value === owner.customRegistry }?.key
        val source = "$registry/${owner.identifier}/${escape(key)}"
        return try {
            resolve(value, mutableListOf(), 0).also { resolved ->
                owner.customRegistry.definition.schema?.get(key)?.let { definition ->
                    RegistryValueSchema { name ->
                        registries()[Identifier("selene", "schemas")]?.get(Identifier.parse(name))
                            ?.element?.let { (it as? JsonObject)?.get("definition") }
                    }.validate(resolved, definition)
                }
            }
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid registry value at $source: ${error.message}", error)
        }
    }

    private fun target(value: JsonObject): Target {
        require(value.size == 1) { "A \$ref cannot have sibling properties" }
        val reference = value["\$ref"] as? JsonObject ?: errorArgument("\$ref must be an object")
        require(reference.keys.all { it in setOf("registry", "entry", "path") }) { "Unknown \$ref property" }
        fun string(name: String): String {
            val field = reference[name] as? JsonPrimitive
            require(field != null && field.isString && field.content.isNotEmpty()) { "\$ref.$name must be a non-empty string" }
            return field.content
        }
        val path = reference["path"]?.let {
            require(it is JsonPrimitive && it.isString) { "\$ref.path must be a JSON Pointer string" }
            it.content
        } ?: ""
        require(path.isEmpty() || path.startsWith('/')) { "\$ref.path must be empty or start with /" }
        return Target(Entry(Identifier.parse(string("registry")), Identifier.parse(string("entry"))), path)
    }

    private fun <T> withTarget(value: JsonObject, chain: MutableList<Target>, depth: Int, action: (JsonElement) -> T): T {
        require(depth < 256) { "Registry value reference nesting exceeds 256" }
        val target = target(value)
        require(target !in chain) { "Cyclic registry value reference: ${(chain + target).joinToString(" -> ")}" }
        chain.add(target)
        try {
            val registry = registries()[target.entry.registry]
                ?: errorArgument("Unknown custom registry: ${target.entry.registry}")
            val entry = registry.get(target.entry.entry)
                ?: errorArgument("Missing registry entry: $target")
            val tokens = if (target.path.isEmpty()) emptyList() else target.path.substring(1).split('/').map { token ->
                require(!Regex("~(?![01])").containsMatchIn(token)) { "Invalid JSON Pointer escape: ${target.path}" }
                token.replace("~1", "/").replace("~0", "~")
            }
            return action(select(entry.element, tokens, chain, depth + 1))
        } finally {
            chain.removeAt(chain.lastIndex)
        }
    }

    private fun select(value: JsonElement, tokens: List<String>, chain: MutableList<Target>, depth: Int): JsonElement {
        require(depth < 256) { "Registry value reference nesting exceeds 256" }
        if (tokens.isEmpty()) return value
        if (value is JsonObject && "\$ref" in value) {
            return withTarget(value, chain, depth) { select(it, tokens, chain, depth + 1) }
        }
        val token = tokens.first()
        val child = when (value) {
            is JsonObject -> value[token]
            is JsonArray -> token.takeIf { it.matches(Regex("0|[1-9][0-9]*")) }?.toIntOrNull()?.let(value::getOrNull)
            else -> null
        } ?: errorArgument("Missing JSON Pointer component '$token' (reference chain: ${chain.joinToString(" -> ")})")
        return select(child, tokens.drop(1), chain, depth + 1)
    }

    private fun resolve(value: JsonElement, chain: MutableList<Target>, depth: Int): JsonElement {
        require(depth < 256) { "Registry value reference nesting exceeds 256" }
        return when (value) {
            is JsonObject -> if ("\$ref" in value) {
                withTarget(value, chain, depth) { resolve(it, chain, depth + 1) }
            } else JsonObject(value.mapValues { resolve(it.value, chain, depth + 1) })
            is JsonArray -> JsonArray(value.map { resolve(it, chain, depth + 1) })
            else -> value
        }
    }

    /** Use raw references so missing targets and cycles still participate in invalidation. */
    fun dependencies(value: JsonElement): Set<Entry> = buildSet {
        fun visit(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    if ("\$ref" in node) runCatching { target(node).entry }.getOrNull()?.let(::add)
                    else node.values.forEach(::visit)
                }
                is JsonArray -> node.forEach(::visit)
                else -> Unit
            }
        }
        visit(value)
    }

    private fun containsReference(value: JsonElement): Boolean = when (value) {
        is JsonObject -> "\$ref" in value || value.values.any(::containsReference)
        is JsonArray -> value.any(::containsReference)
        else -> false
    }

    private fun errorArgument(message: String): Nothing = throw IllegalArgumentException(message)
    private fun escape(value: String) = value.replace("~", "~0").replace("/", "~1")
}

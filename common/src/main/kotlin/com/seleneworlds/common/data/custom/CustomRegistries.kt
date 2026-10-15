package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.Json
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.data.json.FileBasedRegistry
import kotlin.collections.iterator

class CustomRegistries(
    json: Json
) : FileBasedRegistry<CustomRegistryDefinition>(
    json,
    "common",
    "registries",
    CustomRegistryDefinition::class,
    CustomRegistryDefinition.serializer()
) {

    private val customRegistries: MutableMap<Identifier, CustomRegistry> = mutableMapOf()
    private val valueReferences = RegistryValueReferences { customRegistries }
    private var propagating = false
    private val pendingChanges = mutableSetOf<RegistryValueReferences.Entry>()

    private fun invalidateDependents(changed: Set<RegistryValueReferences.Entry>) {
        pendingChanges.addAll(changed)
        if (propagating || pendingChanges.isEmpty()) return
        propagating = true
        try {
            while (pendingChanges.isNotEmpty()) {
                val next = pendingChanges.toSet()
                pendingChanges.clear()
                notifyDependents(next)
            }
        } finally {
            propagating = false
        }
    }

    private fun notifyDependents(changed: Set<RegistryValueReferences.Entry>) {
        // Compute the complete closure before emitting events, visiting cyclic graphs only once.
        val reverse = mutableMapOf<RegistryValueReferences.Entry, MutableSet<RegistryValueReferences.Entry>>()
        customRegistries.forEach { (registryId, registry) ->
            registry.getAll().forEach { (entryId, entry) ->
                valueReferences.dependencies(entry.element).forEach { dependency ->
                    reverse.getOrPut(dependency) { mutableSetOf() }.add(RegistryValueReferences.Entry(registryId, entryId))
                }
            }
        }
        val visited = changed.toMutableSet()
        val queue = ArrayDeque(changed)
        val affected = mutableListOf<RegistryValueReferences.Entry>()
        while (queue.isNotEmpty()) {
            reverse[queue.removeFirst()].orEmpty().forEach { dependent ->
                if (visited.add(dependent)) {
                    affected.add(dependent)
                    queue.addLast(dependent)
                }
            }
        }
        affected.forEach { customRegistries[it.registry]?.invalidateValueReferences(it.entry) }
    }

    private fun createRegistry(identifier: Identifier, definition: CustomRegistryDefinition): CustomRegistry =
        CustomRegistry(json, definition, valueReferences).also { registry ->
            registry.addReloadListener(object : RegistryReloadListener<CustomRegistryObject> {
                private fun changed(entry: Identifier) = invalidateDependents(setOf(RegistryValueReferences.Entry(identifier, entry)))
                override fun onEntryAdded(registry: Registry<CustomRegistryObject>, identifier: Identifier, newData: CustomRegistryObject) = changed(identifier)
                override fun onEntryChanged(registry: Registry<CustomRegistryObject>, identifier: Identifier, oldData: CustomRegistryObject, newData: CustomRegistryObject) {
                    // Derived invalidation is already included in the current transitive closure.
                    if (oldData !== newData) changed(identifier)
                }
                override fun onEntryRemoved(registry: Registry<CustomRegistryObject>, identifier: Identifier, oldData: CustomRegistryObject) = changed(identifier)
                override fun onRegistryReloaded(registry: Registry<CustomRegistryObject>) {
                    // Include removed targets, which can still be named by dependent entries.
                    val targets = customRegistries.values.flatMap { it.getAll().values }
                        .flatMap { valueReferences.dependencies(it.element) }.filter { it.registry == identifier }.toSet()
                    invalidateDependents(targets)
                }
            })
        }

    fun loadCustomRegistries(bundleDatabase: BundleDatabase, platform: String) {
        val definitions = entries.filterValues { it.platform == platform }
        val removedTargets = customRegistries.filter { (identifier, registry) ->
            registry.definition.platform == platform && definitions[identifier]?.name != registry.name
        }.keys.flatMap { registryId ->
            customRegistries.values.flatMap { registry -> registry.getAll().values }
                .flatMap { valueReferences.dependencies(it.element) }.filter { it.registry == registryId }
        }.toSet()
        customRegistries.entries.removeIf { (identifier, registry) ->
            registry.definition.platform == platform && definitions[identifier]?.name != registry.name
        }

        // Publish all registries before loading any entries; cross-registry lookup must not depend on load order.
        for ((identifier, definition) in definitions) {
            customRegistries.getOrPut(identifier) { createRegistry(identifier, definition) }
                .definition = definition
        }
        for ((identifier, _) in definitions) {
            val dynamicRegistry = customRegistries.getValue(identifier)
            dynamicRegistry.load(bundleDatabase)
            customRegistries[identifier] = dynamicRegistry
        }
        invalidateDependents(removedTargets)
    }

    fun getCustomRegistry(identifier: Identifier): Registry<*>? {
        return customRegistries[identifier]
    }

    fun getAllCustomRegistries(): Collection<CustomRegistry> {
        return customRegistries.values
    }

    fun getCustomRegistryEntries(): Map<Identifier, CustomRegistry> = customRegistries.toMap()

    fun findByRegistryName(name: String): CustomRegistry? {
        return customRegistries.values.find { it.name == name }
    }

    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("registries")
    }
}

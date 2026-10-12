package com.seleneworlds.server.entities

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.CacheableRegistry
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.entities.EntityDefinition
import java.util.TreeMap

/** Main-thread index of spawned entities, ordered like EntityManager's entity map. */
internal class EntityTagIndex(private val eligible: (Entity) -> Boolean = { it.dimension != null }) {
    private class Entry(val entity: Entity, val order: Long) {
        var registry: Registry<EntityDefinition>? = null
        var spawned = false
        var tags: Set<String> = emptySet()
    }
    private class Watch(val listener: RegistryReloadListener<EntityDefinition>, var users: Int = 0, var cacheKey: Long? = null)
    private val entries = mutableMapOf<Int, Entry>()
    private val byTag = mutableMapOf<String, TreeMap<Long, Entity>>()
    private val watches = mutableMapOf<Registry<EntityDefinition>, Watch>()
    private var nextOrder = 0L
    private var dirty = false
    private val listeners = mutableMapOf<String, MutableList<(List<Entity>) -> Unit>>()
    private var rebuilding = false
    private var batching = false
    private val pendingTags = mutableSetOf<String>()

    fun batch(callback: () -> Unit) {
        batching = true
        try { callback() } finally {
            batching = false
            val tags = pendingTags.toSet()
            pendingTags.clear()
            publish(tags)
        }
    }

    fun subscribe(tag: String, listener: (List<Entity>) -> Unit): () -> Unit {
        find(tag)
        listeners.getOrPut(tag) { mutableListOf() }.add(listener)
        listener(byTag[tag]?.values?.toList() ?: emptyList())
        return {
            listeners[tag]?.let { bucket ->
                bucket.remove(listener)
                if (bucket.isEmpty()) listeners.remove(tag)
            }
        }
    }

    private fun publish(tags: Set<String>) {
        if (rebuilding) return
        if (batching) { pendingTags.addAll(tags); return }
        tags.forEach { tag ->
            listeners[tag]?.toList()?.forEach { it(byTag[tag]?.values?.toList() ?: emptyList()) }
        }
    }

    private fun invalidate() {
        dirty = true
        // Live tables must update even when their consumers never query again.
        listeners.keys.firstOrNull()?.let { find(it) }
    }

    fun add(entity: Entity) {
        val previous = entries[entity.networkId]
        if (previous != null) remove(entity.networkId)
        val entry = Entry(entity, previous?.order ?: nextOrder++)
        entries[entity.networkId] = entry
        watchDefinition(entry)
        refresh(entry)
    }

    fun remove(id: Int) {
        val entry = entries.remove(id) ?: return
        erase(entry)
        unwatchDefinition(entry)
        publish(entry.tags)
    }

    fun locationChanged(entity: Entity) {
        val entry = entries[entity.networkId]?.takeIf { it.entity === entity } ?: return
        // Moving between positions or two live dimensions does not change tag membership.
        if (entry.spawned != eligible(entity)) refresh(entry)
    }

    fun definitionChanged(entity: Entity) {
        val entry = entries[entity.networkId]?.takeIf { it.entity === entity } ?: return
        unwatchDefinition(entry)
        watchDefinition(entry)
        refresh(entry)
    }

    fun find(tag: String): List<Entity> {
        refreshDefinitions()
        return byTag[tag]?.values?.toList() ?: emptyList()
    }

    fun refreshDefinitions() {
        // An earlier registry listener may query entities before our listener runs.
        // Revision checks make those queries see the just-published definitions too.
        if (!dirty) dirty = watches.any { (registry, watch) ->
            registry is CacheableRegistry && registry.cacheKey != watch.cacheKey
        }
        if (dirty) {
            rebuilding = true
            byTag.clear()
            entries.values.forEach { entry ->
                entry.tags = emptySet()
                refresh(entry)
            }
            dirty = false
            watches.forEach { (registry, watch) -> watch.cacheKey = (registry as? CacheableRegistry)?.cacheKey }
            rebuilding = false
            publish(listeners.keys.toSet())
        }
    }

    private fun refresh(entry: Entry) {
        val oldTags = entry.tags
        erase(entry)
        entry.spawned = eligible(entry.entity)
        entry.tags = if (entry.spawned) entry.entity.entityDefinition.tags.toSet() else emptySet()
        entry.tags.forEach { tag -> byTag.getOrPut(tag) { TreeMap() }[entry.order] = entry.entity }
        publish(oldTags + entry.tags)
    }

    private fun erase(entry: Entry) {
        entry.tags.forEach { tag ->
            byTag[tag]?.let { bucket ->
                bucket.remove(entry.order)
                if (bucket.isEmpty()) byTag.remove(tag)
            }
        }
    }

    private fun watchDefinition(entry: Entry) {
        val reference = entry.entity.entityDefinition
        if (reference === RegistryReference.Unbound) return
        val registry = reference.registry
        entry.registry = registry
        val watch = watches.getOrPut(registry) {
            val listener = object : RegistryReloadListener<EntityDefinition> {
                override fun onRegistryReloaded(registry: Registry<EntityDefinition>) { invalidate() }
                override fun onEntryAdded(registry: Registry<EntityDefinition>, identifier: Identifier, newData: EntityDefinition) { invalidate() }
                override fun onEntryChanged(registry: Registry<EntityDefinition>, identifier: Identifier, oldData: EntityDefinition, newData: EntityDefinition) { invalidate() }
                override fun onEntryRemoved(registry: Registry<EntityDefinition>, identifier: Identifier, oldData: EntityDefinition) { invalidate() }
            }
            registry.addReloadListener(listener)
            Watch(listener, cacheKey = (registry as? CacheableRegistry)?.cacheKey)
        }
        watch.users++
    }

    private fun unwatchDefinition(entry: Entry) {
        val registry = entry.registry ?: return
        val watch = watches.getValue(registry)
        if (--watch.users == 0) {
            registry.removeReloadListener(watch.listener)
            watches.remove(registry)
        }
        entry.registry = null
    }
}

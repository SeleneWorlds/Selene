package com.seleneworlds.server.entities

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.CacheableRegistry
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.entities.EntityDefinition
import java.util.TreeMap

/** Main-thread index of spawned entities, ordered like EntityManager's entity map. */
internal class EntityTagIndex {
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
    }

    fun locationChanged(entity: Entity) {
        val entry = entries[entity.networkId]?.takeIf { it.entity === entity } ?: return
        // Moving between positions or two live dimensions does not change tag membership.
        if (entry.spawned != (entity.dimension != null)) refresh(entry)
    }

    fun definitionChanged(entity: Entity) {
        val entry = entries[entity.networkId]?.takeIf { it.entity === entity } ?: return
        unwatchDefinition(entry)
        watchDefinition(entry)
        refresh(entry)
    }

    fun find(tag: String): List<Entity> {
        // An earlier registry listener may query entities before our listener runs.
        // Revision checks make those queries see the just-published definitions too.
        if (!dirty) dirty = watches.any { (registry, watch) ->
            registry is CacheableRegistry && registry.cacheKey != watch.cacheKey
        }
        if (dirty) {
            byTag.clear()
            entries.values.forEach { entry ->
                entry.tags = emptySet()
                refresh(entry)
            }
            dirty = false
            watches.forEach { (registry, watch) -> watch.cacheKey = (registry as? CacheableRegistry)?.cacheKey }
        }
        return byTag[tag]?.values?.toList() ?: emptyList()
    }

    private fun refresh(entry: Entry) {
        erase(entry)
        entry.spawned = entry.entity.dimension != null
        entry.tags = if (entry.spawned) entry.entity.entityDefinition.tags.toSet() else emptySet()
        entry.tags.forEach { tag -> byTag.getOrPut(tag) { TreeMap() }[entry.order] = entry.entity }
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
                override fun onRegistryReloaded(registry: Registry<EntityDefinition>) { dirty = true }
                override fun onEntryAdded(registry: Registry<EntityDefinition>, identifier: Identifier, newData: EntityDefinition) { dirty = true }
                override fun onEntryChanged(registry: Registry<EntityDefinition>, identifier: Identifier, oldData: EntityDefinition, newData: EntityDefinition) { dirty = true }
                override fun onEntryRemoved(registry: Registry<EntityDefinition>, identifier: Identifier, oldData: EntityDefinition) { dirty = true }
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

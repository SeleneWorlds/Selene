package com.seleneworlds.server.entities

import party.iroiro.luajava.Lua

/** A mutable, dense Lua array maintained by the engine on membership changes.
 * Consumers must not mutate it; mutations during iteration have ordinary Lua array semantics.
 * Existing members reuse their table values, without a global entity-userdata cache.
 */
internal class EntityCollection(private val lua: Lua) {
    private val reference: Int
    private var members = emptyList<Entity>()

    init {
        lua.newTable()
        reference = lua.ref()
    }

    fun push(target: Lua) {
        target.refGet(reference)
    }

    fun update(entities: List<Entity>) {
        if (members == entities) return
        val top = lua.top
        try {
            push(lua)
            val table = lua.top
            lua.newTable()
            val replacement = lua.top
            val positions = members.withIndex().associate { it.value to it.index + 1 }
            entities.forEachIndexed { index, entity ->
                val previous = positions[entity]
                if (previous != null) lua.rawGetI(table, previous)
                else lua.push(entity.api, Lua.Conversion.NONE)
                lua.rawSetI(replacement, index + 1)
            }
            // Keep table identity stable for consumers retaining it across callbacks.
            entities.indices.forEach { index ->
                lua.rawGetI(replacement, index + 1)
                lua.rawSetI(table, index + 1)
            }
            for (index in entities.size until members.size) {
                lua.pushNil()
                lua.rawSetI(table, index + 1)
            }
            members = entities.toList()
        } finally {
            lua.top = top
        }
    }

    fun close() {
        update(emptyList())
        lua.unref(reference)
    }
}

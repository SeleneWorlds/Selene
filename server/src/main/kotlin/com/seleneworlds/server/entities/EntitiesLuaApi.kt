package com.seleneworlds.server.entities

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.server.data.Registries
import com.seleneworlds.common.util.Disposable

/**
 * Create or lookup entities.
 */
class EntitiesLuaApi(
    private val api: EntitiesApi,
    private val registries: Registries
) : LuaModule, Disposable {
    override val name = "selene.entities"
    private class CollectionSubscription(val collection: EntityCollection, val unsubscribe: () -> Unit)
    private val collections = mutableMapOf<Lua, MutableMap<String, CollectionSubscription>>()

    private val beforeEntityMove = LuaEventSink(EntityEvents.BeforeEntityMove.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        EntityEvents.BeforeEntityMove { entity, coordinate ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.xpCall(2, 0, trace)
        }
    }
    private val entityFell = LuaEventSink(EntityEvents.EntityFell.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        EntityEvents.EntityFell { entity, height ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(height)
            lua.xpCall(2, 0, trace)
        }
    }
    private val beforeEntityTurn = LuaEventSink(EntityEvents.BeforeEntityTurn.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        EntityEvents.BeforeEntityTurn { entity, direction ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(direction, Lua.Conversion.NONE)
            lua.xpCall(2, 0, trace)
        }
    }
    private val entitySteppedOnTile = LuaEventSink(EntityEvents.EntitySteppedOnTile.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        EntityEvents.EntitySteppedOnTile { entity, coordinate ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.xpCall(2, 0, trace)
        }
    }
    private val entitySteppedOffTile = LuaEventSink(EntityEvents.EntitySteppedOffTile.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        EntityEvents.EntitySteppedOffTile { entity, coordinate ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.xpCall(2, 0, trace)
        }
    }

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(EntityApi::class, EntityLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("create", this::create)
        table.register("createTransient", this::createTransient)
        table.register("getAll", this::getAll)
        table.register("findAllByTag", this::findAllByTag)
        table.register("byTag", this::byTag)
        table.register("getByNetworkId", this::getByNetworkId)
        table.register("findByRuntimeData", this::findByRuntimeData)
        table.set("beforeMove", beforeEntityMove)
        table.set("fell", entityFell)
        table.set("beforeTurn", beforeEntityTurn)
        table.set("steppedOnTile", entitySteppedOnTile)
        table.set("steppedOffTile", entitySteppedOffTile)
    }

    private fun create(lua: Lua): Int {
        lua.push(api.create(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun createTransient(lua: Lua): Int {
        lua.push(api.createTransient(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun getByNetworkId(lua: Lua): Int {
        lua.push(api.getByNetworkId(lua.checkInt(1)), Lua.Conversion.NONE)
        return 1
    }

    private fun getAll(lua: Lua): Int {
        lua.push(api.getAll(), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Finds all currently spawned entities whose definition has the given tag.
     *
     * findAllByTag(tag: string) -> Entity[]
     */
    private fun findAllByTag(lua: Lua): Int {
        lua.push(api.findAllByTag(lua.checkString(1)), Lua.Conversion.FULL)
        return 1
    }

    /** Returns the same live, mutable array for this tag. Do not modify it.
     * Membership changes immediately; use findAllByTag when iteration needs a snapshot.
     * byTag(tag: string) -> Entity[]
     */
    private fun byTag(lua: Lua): Int {
        val tag = lua.checkString(1)
        val subscription = collections.getOrPut(lua.mainState) { mutableMapOf() }.getOrPut(tag) {
            val collection = EntityCollection(lua.mainState)
            CollectionSubscription(collection, api.subscribeByTag(tag, collection::update))
        }
        // Also observes registry revisions before later registry listeners execute.
        api.refreshTagCollections()
        subscription.collection.push(lua)
        return 1
    }

    override fun dispose() {
        collections.values.forEach { tags -> tags.values.forEach {
            it.unsubscribe()
            it.collection.close()
        } }
        collections.clear()
    }

    private fun findByRuntimeData(lua: Lua): Int {
        val namespace = lua.checkString(1)
        val key = lua.checkString(2)
        val expected = lua.toAny(3)

        val entity = api.getAll().firstOrNull { entity ->
            val runtimeData = entity.delegate.customLuaData ?: return@firstOrNull false
            val originalTop = lua.top
            try {
                runtimeData.push(lua)
                lua.push(namespace)
                lua.rawGet(-2)
                if (!lua.isTable(-1)) return@firstOrNull false
                lua.push(key)
                lua.rawGet(-2)
                lua.toAny(-1) == expected
            } finally {
                lua.top = originalTop
            }
        }

        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }
}

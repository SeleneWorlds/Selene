package com.seleneworlds.common.data

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.event.EventFactory
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ScriptTrace

/**
 * Lookup entries in game registries.
 */
class RegistriesLuaApi(private val api: RegistriesApi) : LuaModule {
    override val name = "selene.registries"
    private val signals = mutableMapOf<Registry<Any>, RegistrySignals>()

    override fun register(table: LuaValue) {
        table.register("add", this::add)
        table.register("remove", this::remove)
        table.register("findAll", this::findAll)
        table.register("findByMetadata", this::findByMetadata)
        table.register("findByName", this::findByName)
        table.register("entryAdded", this::entryAdded)
        table.register("entryChanged", this::entryChanged)
        table.register("entryRemoved", this::entryRemoved)
        table.register("reloaded", this::reloaded)
    }

    private fun findAll(lua: Lua): Int {
        lua.push(api.findAll(lua.checkString(1)), Lua.Conversion.FULL)
        return 1
    }

    private fun findByMetadata(lua: Lua): Int {
        val value = lua.toAny(3) ?: return lua.error(IllegalArgumentException("Value must not be nil"))
        val found = api.findByMetadata(lua.checkString(1), lua.checkString(2), value)
        if (found != null) {
            lua.push(found, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun findByName(lua: Lua): Int {
        val element = api.findByName(lua.checkString(1), lua.checkString(2))
        if (element != null) {
            lua.push(element, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun remove(lua: Lua): Int {
        api.remove(lua.checkString(1), lua.checkString(2))
        return 0
    }

    private fun add(lua: Lua): Int {
        val data = lua.toAny(3) ?: return lua.error(IllegalArgumentException("Data must not be nil"))
        val sourcePath = if (lua.top >= 4 && lua.type(4) != Lua.LuaType.NIL) lua.checkString(4) else null
        val element = api.add(lua.checkString(1), lua.checkString(2), data, sourcePath)
        if (element != null) {
            lua.push(element, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSignals(lua: Lua): RegistrySignals {
        val registry = api.findRegistry(lua.checkString(1)) as Registry<Any>
        return signals.getOrPut(registry) { createSignals(registry) }
    }

    private fun entryAdded(lua: Lua): Int {
        lua.push(getSignals(lua).entryAdded, Lua.Conversion.NONE)
        return 1
    }

    private fun entryChanged(lua: Lua): Int {
        lua.push(getSignals(lua).entryChanged, Lua.Conversion.NONE)
        return 1
    }

    private fun entryRemoved(lua: Lua): Int {
        lua.push(getSignals(lua).entryRemoved, Lua.Conversion.NONE)
        return 1
    }

    private fun reloaded(lua: Lua): Int {
        lua.push(getSignals(lua).reloaded, Lua.Conversion.NONE)
        return 1
    }

    private fun createSignals(registry: Registry<Any>): RegistrySignals {
        val addedEvent = EventFactory.arrayBackedEvent<EntryAdded> { listeners ->
            EntryAdded { identifier, data -> listeners.forEach { it.invoke(identifier, data) } }
        }
        val changedEvent = EventFactory.arrayBackedEvent<EntryChanged> { listeners ->
            EntryChanged { identifier, oldData, newData -> listeners.forEach { it.invoke(identifier, oldData, newData) } }
        }
        val removedEvent = EventFactory.arrayBackedEvent<EntryRemoved> { listeners ->
            EntryRemoved { identifier, data -> listeners.forEach { it.invoke(identifier, data) } }
        }
        val reloadedEvent = EventFactory.arrayBackedEvent<RegistryReloaded> { listeners ->
            RegistryReloaded { listeners.forEach { it.invoke() } }
        }

        registry.addReloadListener(object : RegistryReloadListener<Any> {
            override fun onRegistryReloaded(registry: Registry<Any>) = reloadedEvent.invoker().invoke()
            override fun onEntryChanged(registry: Registry<Any>, identifier: Identifier, oldData: Any, newData: Any) =
                changedEvent.invoker().invoke(identifier, oldData, newData)
            override fun onEntryRemoved(registry: Registry<Any>, identifier: Identifier, oldData: Any) =
                removedEvent.invoker().invoke(identifier, oldData)
            override fun onEntryAdded(registry: Registry<Any>, identifier: Identifier, newData: Any) =
                addedEvent.invoker().invoke(identifier, newData)
        })

        return RegistrySignals(
            LuaEventSink(addedEvent, ::createAddedListener),
            LuaEventSink(changedEvent, ::createChangedListener),
            LuaEventSink(removedEvent, ::createRemovedListener),
            LuaEventSink(reloadedEvent, ::createReloadedListener)
        )
    }

    private fun createAddedListener(callback: LuaValue, trace: ScriptTrace) = EntryAdded { identifier, data ->
        invokeLua(callback, trace, identifier, data)
    }

    private fun createChangedListener(callback: LuaValue, trace: ScriptTrace) = EntryChanged { identifier, oldData, newData ->
        invokeLua(callback, trace, identifier, oldData, newData)
    }

    private fun createRemovedListener(callback: LuaValue, trace: ScriptTrace) = EntryRemoved { identifier, data ->
        invokeLua(callback, trace, identifier, data)
    }

    private fun createReloadedListener(callback: LuaValue, trace: ScriptTrace) = RegistryReloaded {
        invokeLua(callback, trace)
    }

    private fun invokeLua(callback: LuaValue, trace: ScriptTrace, identifier: Identifier, vararg data: Any) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(identifier.toString())
        data.forEach { lua.push(it, Lua.Conversion.NONE) }
        lua.xpCall(data.size + 1, 0, trace)
    }

    private fun invokeLua(callback: LuaValue, trace: ScriptTrace) {
        val lua = callback.state()
        lua.push(callback)
        lua.xpCall(0, 0, trace)
    }

    private data class RegistrySignals(
        val entryAdded: LuaEventSink<EntryAdded>,
        val entryChanged: LuaEventSink<EntryChanged>,
        val entryRemoved: LuaEventSink<EntryRemoved>,
        val reloaded: LuaEventSink<RegistryReloaded>
    )

    fun interface EntryAdded { fun invoke(identifier: Identifier, data: Any) }
    fun interface EntryChanged { fun invoke(identifier: Identifier, oldData: Any, newData: Any) }
    fun interface EntryRemoved { fun invoke(identifier: Identifier, data: Any) }
    fun interface RegistryReloaded { fun invoke() }
}

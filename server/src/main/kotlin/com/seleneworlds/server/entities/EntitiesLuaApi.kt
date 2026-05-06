package com.seleneworlds.server.entities

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.server.data.Registries

/**
 * Create or lookup entities.
 */
class EntitiesLuaApi(
    private val api: EntitiesApi,
    private val registries: Registries
) : LuaModule {
    override val name = "selene.entities"

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
        table.register("getByNetworkId", this::getByNetworkId)
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
}

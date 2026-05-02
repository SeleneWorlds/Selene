package world.selene.server.entities

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.script.ScriptTrace
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall
import world.selene.server.data.Registries

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
        table.register("Create", this::luaCreate)
        table.register("CreateTransient", this::luaCreateTransient)
        table.register("GetByNetworkId", this::luaGetByNetworkId)
        table.set("SteppedOnTile", entitySteppedOnTile)
        table.set("SteppedOffTile", entitySteppedOffTile)
    }

    private fun luaCreate(lua: Lua): Int {
        lua.push(api.create(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateTransient(lua: Lua): Int {
        lua.push(api.createTransient(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetByNetworkId(lua: Lua): Int {
        lua.push(api.getByNetworkId(lua.checkInt(1)), Lua.Conversion.NONE)
        return 1
    }
}

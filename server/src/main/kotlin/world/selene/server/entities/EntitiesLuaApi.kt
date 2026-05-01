package world.selene.server.entities

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.register
import world.selene.server.data.Registries
import world.selene.server.lua.ServerLuaSignals

/**
 * Create or lookup entities.
 */
@Suppress("SameReturnValue")
class EntitiesLuaApi(
    private val api: EntitiesApi,
    private val registries: Registries,
    signals: ServerLuaSignals
) : LuaModule {
    override val name = "selene.entities"

    private val entitySteppedOnTile: Signal = signals.entitySteppedOnTile
    private val entitySteppedOffTile: Signal = signals.entitySteppedOffTile

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

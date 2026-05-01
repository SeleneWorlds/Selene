package world.selene.client.entity

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.data.Registries
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.register

/**
 * Create and lookup entities.
 */
@Suppress("SameReturnValue")
class EntitiesLuaApi(
    private val api: EntitiesApi,
    private val registries: Registries
) : LuaModule {
    override val name = "selene.entities"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("GetEntitiesAt", this::luaGetEntitiesAt)
        table.register("FindEntitiesAt", this::luaFindEntitiesAt)
    }

    private fun luaCreate(lua: Lua): Int {
        lua.push(api.create(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetEntitiesAt(lua: Lua): Int {
        val (coordinate) = lua.checkCoordinate(1)
        lua.push(api.getEntitiesAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindEntitiesAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        lua.checkType(index + 1, Lua.LuaType.TABLE)
        lua.push(api.findEntitiesAt(coordinate, lua.getFieldString(index + 1, "tag")), Lua.Conversion.FULL)
        return 1
    }
}

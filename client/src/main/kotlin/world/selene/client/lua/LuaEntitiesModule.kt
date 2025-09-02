package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.data.Registries
import world.selene.client.maps.ClientMap
import world.selene.client.maps.Entity
import world.selene.client.maps.EntityPool
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldString
import world.selene.common.lua.register
import world.selene.common.util.Coordinate

class LuaEntitiesModule(
    private val entityPool: EntityPool,
    private val registries: Registries,
    private val clientMap: ClientMap
) : LuaModule {
    override val name = "selene.entities"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("GetEntitiesAt", this::luaGetEntitiesAt)
        table.register("FindEntitiesAt", this::luaFindEntitiesAt)
    }

    private fun luaCreate(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityPool.obtain()
        entity.entityDefinition = entityDefinition
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetEntitiesAt(lua: Lua): Int {
        val coordinate = lua.checkUserdata(1, Coordinate::class)
        val entities = clientMap.getEntitiesAt(coordinate)
        lua.push(entities, Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindEntitiesAt(lua: Lua): Int {
        val coordinate = lua.checkUserdata(1, Coordinate::class)
        lua.checkType(2, Lua.LuaType.TABLE)
        val tag = lua.getFieldString(2, "tag")

        val entities = clientMap.getEntitiesAt(coordinate)
        val result = mutableListOf<Entity>()
        for (entity in entities) {
            if (tag != null && !entity.hasTag(tag)) {
                continue
            }
            result.add(entity)
        }
        lua.push(result, Lua.Conversion.FULL)
        return 1
    }

}

package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.register
import world.selene.server.data.Registries
import world.selene.server.entities.EntityManager

class LuaEntitiesModule(private val entityManager: EntityManager, private val registries: Registries, private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.entities"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("CreateTransient", this::luaCreateTransient)
        table.set("SteppedOnTile", signals.entitySteppedOnTile)
        table.set("SteppedOffTile", signals.entitySteppedOffTile)
    }

    private fun luaCreate(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityManager.createEntity(entityDefinition)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateTransient(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityManager.createTransientEntity(entityDefinition)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }
}

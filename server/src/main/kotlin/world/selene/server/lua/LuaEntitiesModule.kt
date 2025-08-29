package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.EntityRegistry
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.server.entities.EntityManager

class LuaEntitiesModule(private val entityManager: EntityManager, private val entityRegistry: EntityRegistry, private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.entities"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("CreateTransient", this::luaCreateTransient)
        table.set("SteppedOnTile", signals.entitySteppedOnTile)
        table.set("SteppedOffTile", signals.entitySteppedOffTile)
    }

    private fun luaCreate(lua: Lua): Int {
        val entityType = lua.checkString(-1)
        val entityDefinition = entityRegistry.get(entityType)
        if (entityDefinition == null) {
            return lua.error(IllegalArgumentException("Unknown entity type: $entityType"))
        }

        val entity = entityManager.createEntity(entityType)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateTransient(lua: Lua): Int {
        val entityType = lua.checkString(1)
        entityRegistry.get(entityType)
            ?: return lua.error(IllegalArgumentException("Unknown entity type: $entityType"))

        val entity = entityManager.createTransientEntity(entityType)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }
}

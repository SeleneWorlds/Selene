package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.EntityRegistry
import world.selene.common.data.VisualComponent
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.server.entities.Entity
import world.selene.server.entities.EntityManager

class LuaEntitiesModule(private val entityManager: EntityManager, private val entityRegistry: EntityRegistry, private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.entities"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(Entity.EntityLuaProxy::class)
        luaManager.exposeClass(VisualComponent.VisualComponentLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("CreateTransient", this::luaCreateTransient)
        table.set("SteppedOnTile", signals.entitySteppedOnTile)
    }

    private fun luaCreate(lua: Lua): Int {
        val entityType = lua.checkString(-1)
        val entityDefinition = entityRegistry.get(entityType)
        if (entityDefinition == null) {
            return lua.error(IllegalArgumentException("Unknown entity type: $entityType"))
        }

        val entity = entityManager.createEntity(entityType)
        lua.push(entity.luaProxy, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateTransient(lua: Lua): Int {
        val entityType = lua.checkString(-1)
        val entityDefinition = entityRegistry.get(entityType)
        if (entityDefinition == null) {
            return lua.error(IllegalArgumentException("Unknown entity type: $entityType"))
        }

        val entity = entityManager.createTransientEntity(entityType)
        lua.push(entity.luaProxy, Lua.Conversion.NONE)
        return 1
    }
}

package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.maps.Entity
import world.selene.client.maps.EntityPool
import world.selene.common.data.EntityRegistry
import world.selene.common.data.VisualComponent
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register

class LuaEntitiesModule(private val entityPool: EntityPool, private val entityRegistry: EntityRegistry) : LuaModule {
    override val name = "selene.entities"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(Entity.EntityLuaProxy::class)
        luaManager.exposeClass(VisualComponent.VisualComponentLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        val entityType = lua.checkString(-1)
        val entityDefinition = entityRegistry.get(entityType)
        if (entityDefinition == null) {
            return lua.error(IllegalArgumentException("Unknown entity type: $entityType"))
        }

        val entity = entityPool.obtain(entityType)
        entity.setupComponents(emptyMap())
        lua.push(entity.luaProxy, Lua.Conversion.NONE)
        return 1
    }
}

package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.register
import world.selene.server.data.Registries
import world.selene.server.entities.EntityManager

/**
 * Create or lookup entities.
 */
class LuaEntitiesModule(private val entityManager: EntityManager, private val registries: Registries, private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.entities"

    /**
     * Fired when an entity steps on a tile.
     */
    private val entitySteppedOnTile: Signal = signals.entitySteppedOnTile

    /**
     * Fired when an entity steps off a tile.
     */
    private val entitySteppedOffTile: Signal = signals.entitySteppedOffTile

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("CreateTransient", this::luaCreateTransient)
        table.register("GetByNetworkId", this::luaGetByNetworkId)
        table.set("SteppedOnTile", entitySteppedOnTile)
        table.set("SteppedOffTile", entitySteppedOffTile)
    }

    /**
     * Creates a new entity from an entity definition.
     * The entity will be saved and synchronized with clients.
     *
     * ```signatures
     * Create(entityDef: string) -> Entity
     * Create(entityDef: EntityDefinition) -> Entity
     * ```
     */
    private fun luaCreate(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityManager.createEntity(entityDefinition)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Creates a new transient entity from an entity definition.
     * Transient entities are not assigned a network id - spawning them is fire-and-forget.
     *
     * ```signatures
     * CreateTransient(entityDef: string) -> Entity|nil
     * CreateTransient(entityDef: EntityDefinition) -> Entity|nil
     * ```
     */
    private fun luaCreateTransient(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityManager.createTransientEntity(entityDefinition)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Retrieves an entity by its network ID.
     * Returns the entity if found, otherwise returns nil.
     *
     * ```signatures
     * GetByNetworkId(networkId: number) -> Entity|nil
     * ```
     */
    private fun luaGetByNetworkId(lua: Lua): Int {
        val networkId = lua.checkInt(1)
        val entity = entityManager.getEntityByNetworkId(networkId)
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }
}

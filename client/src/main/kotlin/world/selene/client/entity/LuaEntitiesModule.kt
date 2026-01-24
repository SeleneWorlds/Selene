package world.selene.client.entity

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.data.Registries
import world.selene.client.maps.ClientMap
import world.selene.common.lua.*
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.register

/**
 * Create and lookup entities.
 */
@Suppress("SameReturnValue")
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

    /**
     * Creates a new entity from an entity definition.
     *
     * ```signatures
     * Create(entityDefinition: EntityDefinition) -> Entity
     * ```
     */
    private fun luaCreate(lua: Lua): Int {
        val entityDefinition = lua.checkRegistry(1, registries.entities)
        val entity = entityPool.obtain()
        entity.entityDefinition = entityDefinition.asReference
        lua.push(entity, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Gets all entities at the specified coordinate.
     *
     * ```signatures
     * GetEntitiesAt(coordinate: Coordinate) -> table[Entity]
     * ```
     */
    private fun luaGetEntitiesAt(lua: Lua): Int {
        val (coordinate) = lua.checkCoordinate(1)
        val entities = clientMap.getEntitiesAt(coordinate)
        lua.push(entities, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Finds entities at a coordinate matching specified criteria.
     *
     * ```signatures
     * FindEntitiesAt(coordinate: Coordinate, criteria: table{tag: string}) -> table[Entity]
     * ```
     */
    private fun luaFindEntitiesAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        lua.checkType(index + 1, Lua.LuaType.TABLE)
        val tag = lua.getFieldString(index + 1, "tag")

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

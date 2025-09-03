package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.RegistryProvider

/**
 * Provides access to game registries.
 */
class LuaRegistriesModule(private val registryProvider: RegistryProvider) : LuaModule {
    override val name = "selene.registries"

    override fun register(table: LuaValue) {
        table.register("FindAll", this::luaFindAll)
        table.register("FindByMetadata", this::luaFindByMetadata)
        table.register("FindByName", this::luaFindByName)
    }

    /**
     * Returns all entries in the specified registry.
     *
     * ```lua
     * table(CustomRegistryObject) FindAll(string registryName)
     * table(TransitionDefinition) FindAll("transitions")
     * table(AudioDefinition) FindAll("audio")
     * table(VisualDefinition) FindAll("visuals")
     * table(TileDefinition) FindAll("tiles")
     * table(ComponentDefinition) FindAll("components")
     * table(SoundsDefinition) FindAll("sounds")
     * table(EntityDefinition) FindAll("entities")
     * ```
     */
    private fun luaFindAll(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        val list = registry.getAll()
        lua.push(list, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Finds the first registry entry that has matching metadata.
     * Returns the entry if found, otherwise returns `nil`.
     *
     * ```lua
     * CustomRegistryObject|nil FindByMetadata(string registryName, string key, any value)
     * TransitionDefinition|nil FindByMetadata("transitions", string key, any value)
     * AudioDefinition|nil FindByMetadata("audio", string key, any value)
     * VisualDefinition|nil FindByMetadata("visuals", string key, any value)
     * TileDefinition|nil FindByMetadata("tiles", string key, any value)
     * ComponentDefinition|nil FindByMetadata("components", string key, any value)
     * SoundsDefinition|nil FindByMetadata("sounds", string key, any value)
     * EntityDefinition|nil FindByMetadata("entities", string key, any value)
     * ```
     */
    private fun luaFindByMetadata(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val key = lua.checkString(2)
        val value = lua.toAny(3)
            ?: return lua.error(IllegalArgumentException("Value must not be nil"))

        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))

        val found = registry.findByMetadata(key, value)
        if (found != null) {
            lua.push( found.second, Lua.Conversion.NONE)
            return 1
        }
        lua.pushNil()
        return 1
    }

    /**
     * Finds a registry entry by its exact name.
     * Returns the entry if found, otherwise returns `nil`.
     *
     * ```lua
     * CustomRegistryObject|nil FindByName(string registryName, string name)
     * TransitionDefinition|nil FindByName("transitions", string name)
     * AudioDefinition|nil FindByName("audio", string name)
     * VisualDefinition|nil FindByName("visuals", string name)
     * TileDefinition|nil FindByName("tiles", string name)
     * ComponentDefinition|nil FindByName("components", string name)
     * SoundsDefinition|nil FindByName("sounds", string name)
     * EntityDefinition|nil FindByName("entities", string name)
     * ```
     */
    private fun luaFindByName(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val name = lua.checkString(2)
        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        val element = registry.get(name) ?: return lua.pushNil().let { 1 }
        lua.push(element, Lua.Conversion.NONE)
        return 1
    }

}

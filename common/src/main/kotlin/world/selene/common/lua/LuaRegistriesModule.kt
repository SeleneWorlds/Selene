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
     * ```signatures
     * FindAll(registryName: string) -> table[CustomRegistryObject]
     * FindAll("transitions": string) -> table[TransitionDefinition]
     * FindAll("audio": string) -> table[AudioDefinition]
     * FindAll("visuals": string) -> table[VisualDefinition]
     * FindAll("tiles": string) -> table[TileDefinition]
     * FindAll("components": string) -> table[ComponentDefinition]
     * FindAll("sounds": string) -> table[SoundsDefinition]
     * FindAll("entities": string) -> table[EntityDefinition]
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
     * ```signatures
     * FindByMetadata(registryName: string, key: string, value: any) -> CustomRegistryObject|nil
     * FindByMetadata("transitions": string, key: string, value: any) -> TransitionDefinition|nil
     * FindByMetadata("audio": string, key: string, value: any) -> AudioDefinition|nil
     * FindByMetadata("visuals": string, key: string, value: any) -> VisualDefinition|nil
     * FindByMetadata("tiles": string, key: string, value: any) -> TileDefinition|nil
     * FindByMetadata("components": string, key: string, value: any) -> ComponentDefinition|nil
     * FindByMetadata("sounds": string, key: string, value: any) -> SoundsDefinition|nil
     * FindByMetadata("entities": string, key: string, value: any) -> EntityDefinition|nil
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
     * ```signatures
     * FindByName(registryName: string, name: string) -> CustomRegistryObject|nil
     * FindByName("transitions": string, name: string) -> TransitionDefinition|nil
     * FindByName("audio": string, name: string) -> AudioDefinition|nil
     * FindByName("visuals": string, name: string) -> VisualDefinition|nil
     * FindByName("tiles": string, name: string) -> TileDefinition|nil
     * FindByName("components": string, name: string) -> ComponentDefinition|nil
     * FindByName("sounds": string, name: string) -> SoundsDefinition|nil
     * FindByName("entities": string, name: string) -> EntityDefinition|nil
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

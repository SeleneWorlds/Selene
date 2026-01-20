package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import party.iroiro.luajava.Lua
import world.selene.common.lua.*
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

interface RegistryObject<T : Any> : LuaMetatableProvider {
    @get:JsonIgnore
    val identifier: Identifier

    @get:JsonIgnore
    val registry: Registry<T>

    @get:JsonIgnore
    val id: Int

    fun initializeFromRegistry(registry: Registry<T>, identifier: Identifier, id: Int)

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Numeric ID (if available), specific to this server installation.
         *
         * ```property
         * Id: number|nil
         * ```
         */
        private fun luaGetId(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            lua.push(registryObject.id)
            return 1
        }

        /**
         * Unique name of this registry object.
         *
         * ```property
         * Name: string
         * ```
         */
        private fun luaGetName(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            lua.push(registryObject.identifier.toString())
            return 1
        }

        /**
         * Gets metadata value for the specified key from this registry object.
         *
         * ```signatures
         * GetMetadata(key: string) -> any|nil
         * ```
         */
        private fun luaGetMetadata(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            val key = lua.checkString(2)
            val value = (registryObject as? MetadataHolder)?.metadata[key]
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }

        /**
         * Checks if this registry object has the specified tag.
         *
         * ```signatures
         * HasTag(tag: string) -> boolean
         * ```
         */
        private fun luaHasTag(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            val tag = lua.checkString(2)
            lua.push((registryObject as? TagHolder)?.tags?.contains(tag) == true)
            return 1
        }

        val luaMeta = LuaMappedMetatable(RegistryObject::class) {
            getter(::luaGetId)
            getter(::luaGetName)
            callable(::luaGetMetadata)
            callable(::luaHasTag)
        }
    }
}

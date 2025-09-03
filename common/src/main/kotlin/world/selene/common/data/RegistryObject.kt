package world.selene.common.data

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

interface RegistryObject<T : Any> : LuaMetatableProvider {
    val name: String
    val registry: Registry<T>
    val id: Int
    fun initializeFromRegistry(registry: Registry<T>, name: String, id: Int)

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        private fun luaGetMetadata(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            val key = lua.checkString(2)
            val value = (registryObject as? MetadataHolder)?.metadata[key]
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }

        private fun luaHasTag(lua: Lua): Int {
            val registryObject = lua.checkUserdata<RegistryObject<*>>(1)
            val tag = lua.checkString(2)
            lua.push((registryObject as? TagHolder)?.tags?.contains(tag) == true)
            return 1
        }

        val luaMeta = LuaMappedMetatable(RegistryObject::class) {
            readOnly(RegistryObject<*>::id)
            readOnly(RegistryObject<*>::name)
            callable(::luaGetMetadata)
            callable(::luaHasTag)
        }
    }
}

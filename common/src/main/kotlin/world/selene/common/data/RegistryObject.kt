package world.selene.common.data

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString

interface RegistryObject<T : Any> : LuaMetatableProvider {
    val name: String
    val registry: Registry<T>
    val id: Int
    fun initializeFromRegistry(registry: Registry<T>, name: String, id: Int)

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(RegistryObject::class) {
            readOnly(RegistryObject<*>::id)
            readOnly(RegistryObject<*>::name)
            callable("GetMetadata") { lua ->
                val registryObject = lua.checkSelf()
                if (registryObject is MetadataHolder) {
                    val key = lua.checkString(2)
                    val value = registryObject.metadata[key]
                    lua.push(value, Lua.Conversion.FULL)
                    1
                } else 0
            }
            callable("HasTag") { lua ->
                val registryObject = lua.checkSelf()
                if (registryObject is TagHolder) {
                    val tag = lua.checkString(2)
                    lua.push(registryObject.tags.contains(tag))
                    1
                } else 0
            }
        }
    }
}

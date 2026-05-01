package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import party.iroiro.luajava.Lua
import world.selene.common.lua.*

abstract class RegistryObject<T : Any> : LuaMetatableProvider, RegistryOwnedObject<T>, IdMappedObject {

    @get:JsonIgnore
    override var id: Int = -1

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return RegistryObjectLuaApi.luaMeta
    }

}

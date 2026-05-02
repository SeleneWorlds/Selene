package world.selene.server.attributes

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.register
import world.selene.common.lua.util.throwTypeError

/**
 * Create common attribute filters.
 */
class AttributesLuaApi(private val api: AttributesApi) : LuaModule {
    override val name = "selene.attributes"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(AttributeApi::class, AttributeLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("ClampFilter", this::luaClampFilter)
        table.register("MathOpFilter", this::luaMathOpFilter)
    }

    private fun luaClampFilter(lua: Lua): Int {
        val min = when (lua.type(1)) {
            Lua.LuaType.NUMBER -> lua.checkInt(1)
            Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeApi>(1)
            else -> lua.throwTypeError(1, Lua.LuaType.NUMBER)
        }
        val max = when (lua.type(2)) {
            Lua.LuaType.NUMBER -> lua.checkInt(2)
            Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeApi>(2)
            else -> lua.throwTypeError(2, Lua.LuaType.NUMBER)
        }
        lua.push(api.clampFilter(min, max), Lua.Conversion.NONE)
        return 1
    }

    private fun luaMathOpFilter(lua: Lua): Int {
        val value = when (lua.type(1)) {
            Lua.LuaType.NUMBER -> lua.checkInt(1)
            Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeApi>(1)
            else -> lua.throwTypeError(1, Lua.LuaType.NUMBER)
        }
        lua.push(api.mathOpFilter(value, lua.checkString(2)), Lua.Conversion.NONE)
        return 1
    }
}

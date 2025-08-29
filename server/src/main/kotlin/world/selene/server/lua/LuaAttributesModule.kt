package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.register
import world.selene.common.lua.throwTypeError
import world.selene.server.attribute.Attribute
import world.selene.server.attribute.AttributeClampFilter
import world.selene.server.attribute.AttributeMathOpFilter
import world.selene.server.attribute.IntAttributeClampFilter
import world.selene.server.attribute.IntAttributeMathOpFilter

class LuaAttributesModule : LuaModule {
    override val name = "selene.attributes"

    override fun register(table: LuaValue) {
        table.register("ClampFilter", this::luaClampFilter)
        table.register("MathOpFilter", this::luaMathOpFilter)
    }

    private fun luaClampFilter(lua: Lua): Int {
        val min = when (lua.type(1)) {
            Lua.LuaType.NUMBER -> AttributeClampFilter.ConstantClampValue(lua.checkInt(1))
            Lua.LuaType.USERDATA -> AttributeClampFilter.AttributeClampValue(lua.checkUserdata<Attribute<Int>>(1))
            else -> lua.throwTypeError(1, Lua.LuaType.NUMBER)
        }
        val max = when (lua.type(2)) {
            Lua.LuaType.NUMBER -> AttributeClampFilter.ConstantClampValue(lua.checkInt(2))
            Lua.LuaType.USERDATA -> AttributeClampFilter.AttributeClampValue(lua.checkUserdata<Attribute<Int>>(2))
            else -> lua.throwTypeError(2, Lua.LuaType.NUMBER)
        }
        lua.push(IntAttributeClampFilter(min, max), Lua.Conversion.NONE)
        return 1
    }

    private fun luaMathOpFilter(lua: Lua): Int {
        val value = when (lua.type(1)) {
            Lua.LuaType.NUMBER -> AttributeMathOpFilter.ConstantMathOpValue(lua.checkInt(1))
            Lua.LuaType.USERDATA -> AttributeMathOpFilter.AttributeMathOpValue(lua.checkUserdata<Attribute<Int>>(1))
            else -> lua.throwTypeError(1, Lua.LuaType.NUMBER)
        }
        val operator = AttributeMathOpFilter.MathOp.fromSign(lua.checkString(2))
        lua.push(IntAttributeMathOpFilter(value, operator), Lua.Conversion.NONE)
        return 1
    }

}

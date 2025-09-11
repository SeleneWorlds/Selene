package world.selene.server.attributes

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.*
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.register
import world.selene.common.lua.util.throwTypeError
import world.selene.server.attributes.filters.AttributeClampFilter
import world.selene.server.attributes.filters.AttributeMathOpFilter
import world.selene.server.attributes.filters.IntAttributeClampFilter
import world.selene.server.attributes.filters.IntAttributeMathOpFilter

/**
 * Create common attribute filters.
 */
@Suppress("SameReturnValue")
class LuaAttributesModule : LuaModule {
    override val name = "selene.attributes"

    override fun register(table: LuaValue) {
        table.register("ClampFilter", this::luaClampFilter)
        table.register("MathOpFilter", this::luaMathOpFilter)
    }

    /**
     * Creates a clamp filter that restricts attribute values between min and max bounds.
     * Both min and max can be constant numbers or other attributes.
     *
     * ```signatures
     * ClampFilter(min: Attribute|number, max: Attribute|number) -> AttributeFilter
     * ```
     */
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

    /**
     * Creates a math operation filter that applies a simple mathematical operation to attribute values.
     * The value can be a constant number or another attribute.
     *
     * Supported operators: +, -, *, /
     *
     * ```signatures
     * MathOpFilter(value: number, operator: string) -> AttributeFilter
     * MathOpFilter(value: Attribute, operator: string) -> AttributeFilter
     * ```
     */
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

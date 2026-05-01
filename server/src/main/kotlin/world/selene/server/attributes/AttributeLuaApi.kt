package world.selene.server.attributes

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.throwTypeError
import world.selene.common.lua.util.toAny
import world.selene.common.observable.Observable
import world.selene.server.attributes.filters.AttributeFilter
import world.selene.server.attributes.filters.LuaAttributeFilter

object AttributeLuaApi {

    private fun luaGetName(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getName())
        return 1
    }

    private fun luaGetValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getValue(), Lua.Conversion.FULL)
        return 1
    }

    private fun luaSetValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.setValue(lua.toAny(3))
        return 0
    }

    private fun luaGetEffectiveValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getEffectiveValue(), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetOwner(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getOwner(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaAddConstraint(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        val name = lua.checkString(2)
        val registrationSite = lua.getCallerInfo()
        val filter = when (lua.type(3)) {
            Lua.LuaType.FUNCTION -> LuaAttributeFilter(lua.checkFunction(3), registrationSite)
            Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(3)
            else -> lua.throwTypeError(3, AttributeFilter::class)
        }
        attribute.attribute.addConstraint(name, filter)
        lua.push(filter, Lua.Conversion.NONE)
        return 1
    }

    private fun luaRemoveConstraint(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.attribute.removeConstraint(lua.checkString(2))
        return 0
    }

    private fun luaAddModifier(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        val registrationSite = lua.getCallerInfo()
        val name = lua.checkString(2)
        val filter = when (lua.type(3)) {
            Lua.LuaType.FUNCTION -> LuaAttributeFilter(lua.checkFunction(3), registrationSite)
            Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(3)
            else -> lua.throwTypeError(3, AttributeFilter::class)
        }
        attribute.attribute.addModifier(name, filter)
        lua.push(filter, Lua.Conversion.NONE)
        return 1
    }

    private fun luaRemoveModifier(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.attribute.removeModifier(lua.checkString(2))
        return 0
    }

    private fun luaRefresh(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.refresh()
        return 0
    }

    val luaMeta = Observable.luaMeta.extend(AttributeApi::class) {
        getter(::luaGetName)
        getter(::luaGetValue)
        setter(::luaSetValue)
        getter(::luaGetEffectiveValue)
        getter(::luaGetOwner)
        callable(::luaAddConstraint)
        callable(::luaRemoveConstraint)
        callable(::luaAddModifier)
        callable(::luaRemoveModifier)
        callable(::luaRefresh)
    }
}

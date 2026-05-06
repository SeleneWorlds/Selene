package com.seleneworlds.server.attributes

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.throwTypeError
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.observable.Observable
import com.seleneworlds.common.observable.ObservableLuaApi
import com.seleneworlds.server.attributes.filters.AttributeFilter
import com.seleneworlds.server.attributes.filters.LuaAttributeFilter

object AttributeLuaApi {

    private fun getName(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getName())
        return 1
    }

    private fun getValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getValue(), Lua.Conversion.FULL)
        return 1
    }

    private fun setValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.setValue(lua.toAny(2))
        return 0
    }

    private fun getEffectiveValue(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getEffectiveValue(), Lua.Conversion.FULL)
        return 1
    }

    private fun getOwner(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        lua.push(attribute.getOwner(), Lua.Conversion.NONE)
        return 1
    }

    private fun addConstraint(lua: Lua): Int {
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

    private fun removeConstraint(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.attribute.removeConstraint(lua.checkString(2))
        return 0
    }

    private fun addModifier(lua: Lua): Int {
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

    private fun removeModifier(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.attribute.removeModifier(lua.checkString(2))
        return 0
    }

    private fun refresh(lua: Lua): Int {
        val attribute = lua.checkUserdata<AttributeApi>(1)
        attribute.refresh()
        return 0
    }

    val luaMeta = ObservableLuaApi.luaMeta.extend(AttributeApi::class) {
        callable(::getName)
        callable(::getValue)
        callable(::setValue)
        callable(::getEffectiveValue)
        callable(::getOwner)
        callable(::addConstraint)
        callable(::removeConstraint)
        callable(::addModifier)
        callable(::removeModifier)
        callable(::refresh)
    }
}

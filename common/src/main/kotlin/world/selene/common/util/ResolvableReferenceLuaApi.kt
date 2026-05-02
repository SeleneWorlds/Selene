package world.selene.common.util

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

object ResolvableReferenceLuaApi {
    /**
     * Identifier backing this reference.
     *
     * ```property
     * Id: any
     * ```
     */
    private fun getId(lua: Lua): Int {
        val ref = lua.checkUserdata<ResolvableReference<Any, Any>>(1)
        lua.push(ref.id, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Resolves and returns the referenced object.
     *
     * ```signatures
     * Get() -> any|nil
     * ```
     */
    private fun get(lua: Lua): Int {
        val ref = lua.checkUserdata<ResolvableReference<Any, Any>>(1)
        lua.push(ref.resolve(), Lua.Conversion.NONE)
        return 1
    }

    val luaMappings = LuaMappedMetatable(ResolvableReference::class) {
        getter(::getId)
        callable(::get)
    }

    val luaMeta = object: LuaMetatable {
        private fun luaMetatable(lua: Lua): LuaMetatable {
            val resolvableReference = lua.checkUserdata<ResolvableReference<*, *>>(1)
            val element = resolvableReference.resolve()
            return element?.let { LuaManager.findMetatable(lua, it) } ?: luaMappings
        }

        override fun luaCall(lua: Lua): Int {
            return luaMetatable(lua).luaCall(lua)
        }

        override fun luaGet(lua: Lua): Int {
            val key = lua.checkString(2)
            if (luaMappings.has(key)) {
                return luaMappings.luaGet(lua)
            }
            return luaMetatable(lua).luaGet(lua)
        }

        override fun luaSet(lua: Lua): Int {
            return luaMetatable(lua).luaSet(lua)
        }

        override fun luaToString(lua: Lua): String {
            return luaMetatable(lua).luaToString(lua)
        }

        override fun luaEquals(lua: Lua): Boolean {
            return luaMetatable(lua).luaEquals(lua)
        }

        override fun luaTypeName(): String {
            return "ResolvableReference"
        }
    }

}

package world.selene.common.lua

import party.iroiro.luajava.Lua
import kotlin.reflect.KClass

class LuaReference<TID : Any, TObject : Any>(
    private val clazz: KClass<TObject>,
    private val id: TID,
    private val resolver: LuaReferenceResolver<TID, TObject>
) : LuaMetatable {

    fun resolve(): TObject? {
        return resolver.luaDereference(id)
    }

    private fun luaMetatable(lua: Lua): LuaMetatable {
        val element = resolve()
        if (element is LuaMetatable) {
            return element
        } else if (element is LuaMetatableProvider) {
            return element.luaMetatable(lua)
        }
        return luaMeta
    }

    override fun luaCall(lua: Lua): Int {
        return luaMetatable(lua).luaCall(lua)
    }

    override fun luaGet(lua: Lua): Int {
        val key = lua.checkString(2)
        if (luaMeta.has(key)) {
            return luaMeta.luaGet(lua)
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
        return "Ref(${clazz.simpleName}, $id)"
    }

    override fun toString(): String {
        return "Ref(${clazz.simpleName}, $id)"
    }

    companion object {
        val luaMeta = LuaMappedMetatable(LuaReference::class) {
            readOnly(LuaReference<*, *>::id)
            callable("Get") {
                @Suppress("UNCHECKED_CAST") val ref = it.checkSelf() as LuaReference<Any, Any>
                it.push(ref.resolve(), Lua.Conversion.NONE)
                1
            }
        }
    }
}
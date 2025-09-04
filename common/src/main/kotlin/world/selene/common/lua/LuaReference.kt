package world.selene.common.lua

import party.iroiro.luajava.Lua
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

class LuaReference<TID : Any, TObject : Any>(
    private val clazz: KClass<TObject>,
    private val id: TID,
    private val resolver: LuaReferenceResolver<TID, TObject>,
    var initialObject: TObject? = null
) : LuaMetatable {

    private var cachedObject: WeakReference<TObject>? = null

    init {
        if (initialObject != null) {
            cachedObject = WeakReference(initialObject)
        }
    }

    fun resolve(): TObject? {
        if (cachedObject != null) {
            val resolved = cachedObject?.get()
            if (resolved != null) {
                return resolved
            } else {
                cachedObject = null
            }
        }
        val resolved = resolver.luaDereference(id)
        cachedObject = WeakReference(resolved)
        return resolved
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
        /**
         * Resolves and returns the referenced object.
         * 
         * ```signatures
         * Get() -> any|nil
         * ```
         */
        private fun luaGet(lua: Lua): Int {
            val ref = lua.checkUserdata<LuaReference<Any, Any>>(1)
            lua.push(ref.resolve(), Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(LuaReference::class) {
            readOnly(LuaReference<*, *>::id)
            callable(::luaGet)
        }
    }
}
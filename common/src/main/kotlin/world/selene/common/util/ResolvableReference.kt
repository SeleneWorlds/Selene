package world.selene.common.util

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaManager
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

class ResolvableReference<TID : Any, TObject : Any>(
    private val clazz: KClass<TObject>,
    private val id: TID,
    private val resolver: ReferenceResolver<TID, TObject>,
    initialObject: TObject? = null
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
        val resolved = resolver.dereferencePersisted(id)
        cachedObject = WeakReference(resolved)
        return resolved
    }

    private fun luaMetatable(lua: Lua): LuaMetatable {
        val element = resolve()
        return element?.let { LuaManager.findMetatable(lua, it) } ?: luaMeta
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

    @Suppress("SameReturnValue", "unused")
    companion object {
        /**
         * Identifier backing this reference.
         *
         * ```property
         * Id: any
         * ```
         */
        private fun luaGetId(lua: Lua): Int {
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
        private fun luaGet(lua: Lua): Int {
            val ref = lua.checkUserdata<ResolvableReference<Any, Any>>(1)
            lua.push(ref.resolve(), Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(ResolvableReference::class) {
            getter(::luaGetId)
            callable(::luaGet)
        }
    }
}

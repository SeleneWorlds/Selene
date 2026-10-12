package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import java.util.WeakHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.isAccessible

open class LuaMappedMetatable<T : Any>(private val clazz: KClass<T>, body: (LuaMappedMetatable<T>.() -> Unit)) :
    LuaMetatable {
    private class CallableBinding(val method: KFunction<*>, val version: Long)
    private class CachedCallable(val version: Long, val reference: Int)

    private val callables = mutableMapOf<String, CallableBinding>()
    private val getters = mutableMapOf<String, KFunction<*>>()
    private val setters = mutableMapOf<String, KFunction<*>>()
    private var callableVersion = 0L

    // References are integers owned by the native registry, not LuaValues (which retain their state).
    // Weak main-state keys avoid retaining closed states; closing Lua frees the registry references.
    // Coroutines share the main state's registry. Separate states may use this metatable concurrently.
    private val callableCaches = WeakHashMap<Lua, MutableMap<String, CachedCallable>>()

    init {
        body()
    }

    fun getter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: function.name.removePrefix("luaGet").removePrefix("get")
        getters[key] = function
        function.isAccessible = true
    }

    fun setter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: function.name.removePrefix("luaSet").removePrefix("set")
        setters[key] = function
        function.isAccessible = true
    }

    fun callable(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: function.name.removePrefix("lua")
        callables[key] = CallableBinding(function, ++callableVersion)
        function.isAccessible = true
    }

    override fun luaGet(lua: Lua): Int {
        val key = lua.checkString(2)

        callables[key]?.let { binding ->
            val cache = synchronized(callableCaches) {
                callableCaches.getOrPut(lua.mainState) { mutableMapOf() }
            }
            val cached = cache[key]
            if (cached != null && cached.version == binding.version) {
                lua.refGet(cached.reference)
                return 1
            }

            // A re-registered method replaces the cache entry on its next lookup in each state.
            // Lua code retaining the old function still sees the old implementation.
            lua.push { state -> binding.method.call(state) as Int }
            lua.pushValue(-1)
            cache[key] = CachedCallable(binding.version, lua.ref())
            if (cached != null) lua.unref(cached.reference)
            return 1
        }

        getters[key]?.let { getter ->
            return getter.call(lua) as Int
        }

        lua.pushNil()
        return 1
    }

    override fun luaSet(lua: Lua): Int {
        val key = lua.checkString(2)

        setters[key]?.let { setter ->
            return setter.call(lua) as Int
        }

        return lua.error(NoSuchFieldException("Property '$key' does not exist on ${luaTypeName()}"))
    }

    override fun luaTypeName(): String {
        return clazz.simpleName ?: "(anonymous class)"
    }

    override fun luaToString(lua: Lua): String {
        return lua.checkSelf().toString()
    }

    fun Lua.checkSelf(): T {
        return checkUserdata(1, clazz)
    }

    fun <SubType : T> extend(
        clazz: KClass<SubType>,
        body: (LuaMappedMetatable<SubType>.() -> Unit)
    ): LuaMappedMetatable<SubType> {
        val source = this
        return LuaMappedMetatable(clazz) {
            for (entry in source.callables) {
                callable(entry.value.method, entry.key)
            }
            for (entry in source.getters) {
                getter(entry.value, entry.key)
            }
            for (entry in source.setters) {
                setter(entry.value, entry.key)
            }
        }.apply(body)
    }

    fun has(key: String): Boolean {
        return callables.containsKey(key) || getters.containsKey(key)
    }
}

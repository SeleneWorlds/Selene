package world.selene.common.lua

import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.isAccessible

open class LuaMappedMetatable<T : Any>(private val clazz: KClass<T>, body: (LuaMappedMetatable<T>.() -> Unit)) :
    LuaMetatable {
    private val callables = mutableMapOf<String, KFunction<*>>()
    private val getters = mutableMapOf<String, KFunction<*>>()
    private val setters = mutableMapOf<String, KFunction<*>>()

    init {
        body()
    }

    private fun capitalize(s: String): String {
        return s.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    fun getter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: capitalize(function.name.removePrefix("luaGet").removePrefix("get"))
        getters[key] = function
        function.isAccessible = true
    }

    fun setter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: capitalize(function.name.removePrefix("luaSet").removePrefix("set"))
        setters[key] = function
        function.isAccessible = true
    }

    fun callable(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 1) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.returnType.classifier == Int::class) { "Method '${function.name}' has invalid return type: ${function.returnType}" }
        val key = alias ?: capitalize(function.name.removePrefix("lua"))
        callables[key] = function
        function.isAccessible = true
    }

    override fun luaGet(lua: Lua): Int {
        val key = lua.checkString(2)

        callables[key]?.let { method ->
            lua.push { lua ->
                method.call(lua) as Int
            }
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
                callable(entry.value)
            }
            for (entry in source.getters) {
                getter(entry.value)
            }
            for (entry in source.setters) {
                setter(entry.value)
            }
        }.apply(body)
    }

    fun has(key: String): Boolean {
        return callables.containsKey(key) || getters.containsKey(key)
    }
}
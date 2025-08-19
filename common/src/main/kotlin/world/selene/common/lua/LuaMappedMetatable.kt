package world.selene.common.lua

import party.iroiro.luajava.Lua
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.javaType
import kotlin.reflect.jvm.isAccessible

class LuaMappedMetatable<T : Any>(private val clazz: KClass<T>, body: (LuaMappedMetatable<T>.() -> Unit)) : LuaMetatable {
    private val properties = mutableMapOf<String, KProperty<*>>()
    private val mutableProperties = mutableMapOf<String, KMutableProperty<*>>()
    private val callables = mutableMapOf<String, KFunction<*>>()
    private val inlineCallables = mutableMapOf<String, (Lua) -> Int>()
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

    fun readOnly(property: KProperty<*>, alias: String? = null) {
        val key = alias ?: capitalize(property.name)
        properties[key] = property
        property.isAccessible = true
    }

    fun getter(function: KFunction<*>) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 2) { "Getter '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == clazz) { "Getter '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.parameters[1].type.classifier == Lua::class) { "Getter '${function.name}' has invalid parameter type: ${function.parameters[1].type}" }
        require(function.returnType.classifier == Int::class) { "Getter '${function.name}' has invalid return type: ${function.returnType}" }
        getters[capitalize(function.name)] = function
        function.isAccessible = true
    }

    fun setter(function: KFunction<*>) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 2) { "Setter '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == clazz) { "Setter '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        require(function.parameters[1].type.classifier == Lua::class) { "Setter '${function.name}' has invalid parameter type: ${function.parameters[1].type}" }
        require(function.returnType.classifier == Int::class) { "Setter '${function.name}' has invalid return type: ${function.returnType}" }
        setters[capitalize(function.name)] = function
        function.isAccessible = true
    }

    fun writable(property: KMutableProperty<*>, alias: String? = null) {
        val key = alias ?: capitalize(property.name)
        mutableProperties[key] = property
        properties[key] = property
        property.isAccessible = true
    }

    fun callable(function: KFunction<*>) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size <= 2) { "Method '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(function.parameters[0].type.classifier == clazz) { "Method '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        if (function.parameters.size == 2) {
            require(function.parameters[1].type.classifier == Lua::class) { "Method '${function.name}' has invalid parameter type: ${function.parameters[1].type}" }
        }
        callables[capitalize(function.name)] = function
        function.isAccessible = true
    }

    fun callable(key: String, function: (Lua) -> Int) {
        inlineCallables[key] = function
    }

    override fun luaGet(lua: Lua): Int {
        val self = lua.checkJavaObject(1, clazz)
        val key = lua.checkString(2)

        callables[key]?.let { method ->
            lua.push { innerLua ->
                val result = when (method.parameters.size) {
                    1 -> method.call(self)
                    2 -> method.call(self, innerLua)
                    else -> throw IllegalStateException("Method '$key' has invalid number of parameters: ${method.parameters.size}")
                }

                when (result) {
                    is Int -> result
                    is Unit -> 0
                    else -> {
                        innerLua.push(result, Lua.Conversion.FULL)
                        1
                    }
                }
            }
            return 1
        }

        inlineCallables[key]?.let { method ->
            lua.push { innerLua ->
                method(innerLua)
            }
            return 1
        }

        properties[key]?.let { property ->
            val value = property.call(self)
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }

        getters[key]?.let { getter ->
            return getter.call(self, lua) as Int
        }

        lua.pushNil()
        return 1
    }

    override fun luaSet(lua: Lua): Int {
        val self = lua.checkJavaObject(1, clazz)
        val key = lua.checkString(2)

        mutableProperties[key]?.let { property ->
            val value = when (property.returnType.classifier) {
                Boolean::class -> lua.checkBoolean(3)
                Int::class -> lua.checkInt(3)
                String::class -> lua.checkString(3)
                Float::class -> lua.checkFloat(3)
                else -> lua.toObject(3)
            }
            try {
                property.setter.call(self, value)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid value for property '$key' on ${luaTypeName()}: $value (${value?.javaClass})", e)
            }
            return 0
        }

        setters[key]?.let { setter ->
            return setter.call(self, lua) as Int
        }

        properties[key]?.let { property ->
            return lua.error(IllegalAccessError("Property '$key' is read-only on ${luaTypeName()}"))
        }

        return lua.error(NoSuchFieldError("Property '$key' does not exist on ${luaTypeName()}"))
    }

    override fun luaTypeName(): String {
        return clazz.simpleName ?: "(anonymous class)"
    }

    override fun luaToString(lua: Lua): String {
        return lua.checkSelf().toString()
    }

    fun Lua.checkSelf(): T {
        return checkJavaObject(1, clazz)
    }
}
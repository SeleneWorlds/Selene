package world.selene.common.lua

import party.iroiro.luajava.Lua
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class LuaMappedMetatable<T : Any>(private val clazz: KClass<T>, body: (LuaMappedMetatable<T>.() -> Unit)) :
    LuaMetatable {
    private val properties = mutableMapOf<String, KProperty<*>>()
    private val mutableProperties = mutableMapOf<String, KMutableProperty<*>>()
    private val callables = mutableMapOf<String, KFunction<*>>()
    private val inlineCallables = mutableMapOf<String, (Lua) -> Int>()
    private val getters = mutableMapOf<String, KFunction<*>>()
    private val inlineGetters = mutableMapOf<String, (Lua) -> Int>()
    private val setters = mutableMapOf<String, KFunction<*>>()
    private val inlineSetters = mutableMapOf<String, (Lua) -> Int>()

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

    fun getter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size in 1..2) { "Getter '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(clazz.isSubclassOf(function.parameters[0].type.jvmErasure)) { "Getter '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        if (function.parameters.size == 2) {
            require(function.parameters[1].type.classifier == Lua::class) { "Getter '${function.name}' has invalid parameter type: ${function.parameters[1].type}" }
            require(function.returnType.classifier == Int::class) { "Getter '${function.name}' has invalid return type: ${function.returnType}" }
        }
        getters[alias ?: capitalize(function.name.removePrefix("get"))] = function
        function.isAccessible = true
    }

    fun setter(function: KFunction<*>, alias: String? = null) {
        require(!function.isSuspend) { "Suspend functions are not supported" }
        require(function.parameters.size == 2) { "Setter '${function.name}' has invalid number of parameters: ${function.parameters.size}" }
        require(clazz.isSubclassOf(function.parameters[0].type.jvmErasure)) { "Setter '${function.name}' has invalid parameter type: ${function.parameters[0].type}" }
        if (function.parameters[1].type.classifier == Lua::class) {
            require(function.returnType.classifier == Int::class) { "Setter '${function.name}' has invalid return type: ${function.returnType}" }
        }
        setters[alias ?: capitalize(function.name.removePrefix("set"))] = function
        function.isAccessible = true
    }

    fun getter(key: String, function: (Lua) -> Int) {
        inlineGetters[key] = function
    }

    fun setter(key: String, function: (Lua) -> Int) {
        inlineSetters[key] = function
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
        val self = lua.checkUserdata(1, clazz)
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
            return when (getter.parameters.size) {
                1 -> {
                    val value = getter.call(self)
                    lua.push(value, Lua.Conversion.FULL)
                    1
                }

                2 -> {
                    return getter.call(self, lua) as Int
                }

                else -> throw IllegalStateException("Getter '$key' has invalid number of parameters: ${getter.parameters.size}")
            }
        }

        inlineGetters[key]?.let { getter ->
            return getter(lua)
        }

        lua.pushNil()
        return 1
    }

    override fun luaSet(lua: Lua): Int {
        val self = lua.checkUserdata(1, clazz)
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
                throw IllegalArgumentException(
                    "Invalid value for property '$key' on ${luaTypeName()}: $value (${value?.javaClass})",
                    e
                )
            }
            return 0
        }

        setters[key]?.let { setter ->
            if (setter.parameters[1].type.classifier == Lua::class) {
                return setter.call(self, lua) as Int
            } else {
                val value = when (setter.parameters[1].type.classifier) {
                    Boolean::class -> lua.checkBoolean(3)
                    Int::class -> lua.checkInt(3)
                    String::class -> lua.checkString(3)
                    Float::class -> lua.checkFloat(3)
                    else -> lua.toObject(3)
                }
                setter.call(self, value)
                return 0
            }
        }

        inlineSetters[key]?.let { setter ->
            return setter(lua)
        }

        properties[key]?.let { property ->
            return lua.error(IllegalAccessError("Property '$key' is read-only on ${luaTypeName()}"))
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
            for (entry in source.properties) {
                readOnly(entry.value, entry.key)
            }
            for (entry in source.mutableProperties) {
                writable(entry.value, entry.key)
            }
            for (entry in source.callables) {
                callable(entry.value)
            }
            for (entry in source.inlineCallables) {
                callable(entry.key, entry.value)
            }
            for (entry in source.getters) {
                getter(entry.value)
            }
            for (entry in source.setters) {
                setter(entry.value)
            }
        }.apply(body)
    }
}
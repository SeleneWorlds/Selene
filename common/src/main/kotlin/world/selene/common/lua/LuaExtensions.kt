package world.selene.common.lua

import party.iroiro.luajava.AbstractLua
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaFunction
import party.iroiro.luajava.value.LuaValue
import kotlin.math.abs
import kotlin.reflect.KClass

fun LuaValue.register(name: String, value: LuaFunction) {
    set(name, value)
}

fun LuaValue.register(name: String, value: JFunction) {
    set(name, value)
}

fun AbstractLua.register(name: String?, function: JFunction) {
    push(function)
    setGlobal(name)
}

fun Lua.newTable(body: LuaValue.() -> Unit): LuaValue {
    newTable()
    return get().apply(body)
}

fun Lua.luaTableToMap(index: Int): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    pushNil()
    while (next(index - 1) != 0) {
        val key = when (type(-2)) {
            Lua.LuaType.STRING -> toString(-2)
            Lua.LuaType.NUMBER -> toString(-2)
            else -> null
        }
        if (key != null) {
            val value = when (type(-1)) {
                Lua.LuaType.TABLE -> luaTableToMap(-1)
                Lua.LuaType.STRING -> toString(-1)
                Lua.LuaType.NUMBER -> toNumber(-1)
                Lua.LuaType.BOOLEAN -> toBoolean(-1)
                else -> null
            }
            if (value != null) {
                result[key] = value
            }
        }
        pop(1)
    }
    return result
}

fun throwTypeError(index: Int, expectedType: Lua.LuaType, actualType: Lua.LuaType?): Nothing {
    throw IllegalArgumentException("Expected $expectedType, got $actualType at index $index")
}

fun throwTypeError(index: Int, expectedType: KClass<*>, actualType: Lua.LuaType?): Nothing {
    throw IllegalArgumentException("Expected ${expectedType.simpleName}, got $actualType at index $index")
}

fun throwTypeError(index: Int, expectedType: KClass<*>, actualType: KClass<*>): Nothing {
    throw IllegalArgumentException("Expected ${expectedType.simpleName}, got ${actualType.simpleName} at index $index")
}

fun Lua.checkBoolean(index: Int): Boolean {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.BOOLEAN, Lua.LuaType.NIL)
    }
    val type = type(index)
    return when (type) {
        Lua.LuaType.BOOLEAN -> toBoolean(index)
        else -> throwTypeError(index, Lua.LuaType.BOOLEAN, type)
    }
}

fun Lua.checkString(index: Int): String {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.STRING, Lua.LuaType.NIL)
    }
    val type = type(index)
    return when (type) {
        Lua.LuaType.STRING -> toString(index)!!
        else -> throwTypeError(index, Lua.LuaType.STRING, type)
    }
}

fun Lua.checkInt(index: Int): Int {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.NUMBER, Lua.LuaType.NIL)
    }
    val type = type(index)
    return when (type) {
        Lua.LuaType.NUMBER -> toInteger(index).toInt()
        else -> throwTypeError(index, Lua.LuaType.NUMBER, type)
    }
}

fun Lua.checkFloat(index: Int): Float {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.NUMBER, Lua.LuaType.NIL)
    }
    val type = type(index)
    return when (type) {
        Lua.LuaType.NUMBER -> toNumber(index).toFloat()
        else -> throwTypeError(index, Lua.LuaType.NUMBER, type)
    }
}

inline fun <reified T : Any> Lua.checkJavaObject(index: Int): T {
    return checkJavaObject(index, T::class)
}

fun <T : Any> Lua.checkJavaObject(index: Int, clazz: KClass<out T>): T {
    if (top < abs(index)) {
        throwTypeError(index, clazz, Lua.LuaType.NIL)
    }
    val type = type(index)
    val value = when (type) {
        Lua.LuaType.USERDATA -> toJavaObject(index)!!
        else -> throwTypeError(index, clazz, type)
    }
    if (!clazz.isInstance(value)) {
        throwTypeError(index, clazz, value::class)
    }
    @Suppress("UNCHECKED_CAST")
    return value as T
}

inline fun <reified T : Enum<T>> Lua.checkEnum(index: Int): T {
    return checkEnum(index, T::class)
}

fun <T : Enum<T>> Lua.checkEnum(index: Int, clazz: KClass<T>): T {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.STRING, Lua.LuaType.NIL)
    }
    val type = type(index)
    return when (type) {
        Lua.LuaType.USERDATA -> {
            return checkJavaObject(index, clazz)
        }
        Lua.LuaType.STRING -> {
            try {
                enumValueOf(toString(index)!!, clazz)
            } catch (_: IllegalArgumentException) {
                throwTypeError(index, clazz, type)
            }
        }

        else -> throwTypeError(index, Lua.LuaType.STRING, type)
    }
}

private fun <T : Enum<T>> enumValueOf(value: String, clazz: KClass<T>): T {
    return clazz.java.enumConstants.firstOrNull { it.name.equals(value, ignoreCase = true) } as T
}

fun Lua.checkType(index: Int, expectedType: Lua.LuaType) {
    if (top < abs(index)) {
        throwTypeError(index, expectedType, Lua.LuaType.NIL)
    }
    val type = type(index)
    if (type != expectedType) {
        throwTypeError(index, expectedType, type)
    }
}

fun Lua.getFieldString(tableIndex: Int, fieldName: String): String? {
    if (!isTable(tableIndex)) {
        return null
    }

    var result: String? = null
    getField(tableIndex, fieldName)
    if (!isNil(-1)) {
        result = checkString(-1)
    }
    pop(1)
    return result
}

fun Lua.getFieldFloat(tableIndex: Int, fieldName: String): Float? {
    if (!isTable(tableIndex)) {
        return null
    }

    var result: Float? = null
    getField(tableIndex, fieldName)
    if (!isNil(-1)) {
        result = checkFloat(-1)
    }
    pop(1)
    return result
}

fun Lua.getFieldBoolean(tableIndex: Int, fieldName: String): Boolean? {
    if (!isTable(tableIndex)) {
        return null
    }

    var result: Boolean? = null
    getField(tableIndex, fieldName)
    if (!isNil(-1)) {
        result = checkBoolean(-1)
    }
    pop(1)
    return result
}

fun <T: Any> Lua.getFieldJavaObject(tableIndex: Int, fieldName: String, clazz: KClass<out T>): T? {
    if (!isTable(tableIndex)) {
        return null
    }

    var result: T? = null
    getField(tableIndex, fieldName)
    if (!isNil(-1)) {
        result = checkJavaObject(-1, clazz)
    }
    pop(1)
    return result
}
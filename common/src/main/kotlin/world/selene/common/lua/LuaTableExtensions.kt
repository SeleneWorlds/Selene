package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import kotlin.reflect.KClass

fun Lua.newTable(body: LuaValue.() -> Unit): LuaValue {
    newTable()
    return get().apply(body)
}

fun <T> Lua.getField(tableIndex: Int, fieldName: String, handler: (type: Lua.LuaType) -> T): T? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = type(-1)?.let { handler(it) }
    pop(1)
    return result
}

fun Lua.getFieldString(tableIndex: Int, fieldName: String): String? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = when (type(-1)) {
        Lua.LuaType.STRING -> toString(-1)
        Lua.LuaType.NIL -> null
        else -> throwError("Expected a string value for field $fieldName")
    }
    pop(1)
    return result
}

fun Lua.getFieldFloat(tableIndex: Int, fieldName: String): Float? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = when (type(-1)) {
        Lua.LuaType.NUMBER -> toNumber(-1).toFloat()
        Lua.LuaType.NIL -> null
        else -> throwError("Expected a float value for field $fieldName")
    }
    pop(1)
    return result
}

fun Lua.getFieldInt(tableIndex: Int, fieldName: String): Int? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = when (type(-1)) {
        Lua.LuaType.NUMBER -> toInteger(-1).toInt()
        Lua.LuaType.NIL -> null
        else -> throwError("Expected an integer value for field $fieldName")
    }
    pop(1)
    return result
}

fun Lua.getFieldBoolean(tableIndex: Int, fieldName: String): Boolean? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = when (type(-1)) {
        Lua.LuaType.BOOLEAN -> toBoolean(-1)
        Lua.LuaType.NIL -> null
        else -> throwError("Expected a boolean value for field $fieldName")
    }
    pop(1)
    return result
}

fun <T : Any> Lua.getFieldUserdata(tableIndex: Int, fieldName: String, clazz: KClass<out T>): T? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    val result = when (type(-1)) {
        Lua.LuaType.USERDATA -> toUserdata(-1, clazz)
            ?: throwError("Expected a ${clazz.simpleName} value for field $fieldName")

        Lua.LuaType.NIL -> null
        else -> throwError("Expected a ${clazz.simpleName} value for field $fieldName")
    }
    pop(1)
    return result
}

fun Lua.getFieldFunction(tableIndex: Int, fieldName: String): LuaValue? {
    if (!isTable(tableIndex)) {
        return null
    }

    getField(tableIndex, fieldName)
    return when (type(-1)) {
        Lua.LuaType.FUNCTION -> get()
        Lua.LuaType.NIL -> null.also { pop(1) }
        else -> throwError("Expected a function value for field $fieldName")
    }
}
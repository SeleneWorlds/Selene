package world.selene.common.lua

import party.iroiro.luajava.AbstractLua
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.Lua.LuaType
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.LuaException.LuaError
import party.iroiro.luajava.value.LuaFunction
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.Registry
import world.selene.common.grid.Grid
import world.selene.common.util.Coordinate
import java.util.*
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

fun Lua.pushError(message: String): Int {
    val callerInfo = getCallerInfo()
    return error(LuaException(LuaException.LuaError.RUNTIME, "$callerInfo: $message"))
}

fun Lua.throwError(message: String): Nothing {
    val callerInfo = getCallerInfo()
    throw LuaException(LuaException.LuaError.RUNTIME, "$callerInfo: $message")
}

fun Lua.throwTypeError(index: Int, expectedType: Lua.LuaType, actualType: Lua.LuaType? = null): Nothing {
    val actualType = actualType ?: type(index)
    throwError("Expected $expectedType, got $actualType at index $index")
}

fun Lua.throwTypeError(index: Int, expectedType: KClass<*>, actualType: Lua.LuaType? = null): Nothing {
    val actualType = actualType ?: type(index)
    throwError("Expected ${expectedType.simpleName}, got $actualType at index $index")
}

fun Lua.throwTypeError(index: Int, expectedType: KClass<*>, actualType: KClass<*>): Nothing {
    throwError("Expected ${expectedType.simpleName}, got ${actualType.simpleName} at index $index")
}

fun Lua.checkBoolean(index: Int): Boolean {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.BOOLEAN, Lua.LuaType.NIL)
    }
    return when (val type = type(index)) {
        Lua.LuaType.BOOLEAN -> toBoolean(index)
        else -> throwTypeError(index, Lua.LuaType.BOOLEAN, type)
    }
}

fun Lua.checkString(index: Int): String {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.STRING, Lua.LuaType.NIL)
    }
    return when (val type = type(index)) {
        Lua.LuaType.STRING -> toString(index)!!
        else -> throwTypeError(index, Lua.LuaType.STRING, type)
    }
}

fun Lua.checkInt(index: Int): Int {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.NUMBER, Lua.LuaType.NIL)
    }
    return when (val type = type(index)) {
        Lua.LuaType.NUMBER -> toInteger(index).toInt()
        else -> throwTypeError(index, Lua.LuaType.NUMBER, type)
    }
}

fun Lua.checkFloat(index: Int): Float {
    if (top < abs(index)) {
        throwTypeError(index, Lua.LuaType.NUMBER, Lua.LuaType.NIL)
    }
    return when (val type = type(index)) {
        Lua.LuaType.NUMBER -> toNumber(index).toFloat()
        else -> throwTypeError(index, Lua.LuaType.NUMBER, type)
    }
}

inline fun <reified T : Any> Lua.checkUserdata(index: Int): T {
    return checkUserdata(index, T::class)
}

fun <T : Any> Lua.checkUserdata(index: Int, clazz: KClass<out T>): T {
    if (top < abs(index)) {
        throwTypeError(index, clazz, Lua.LuaType.NIL)
    }
    var value = when (val type = type(index)) {
        Lua.LuaType.USERDATA -> toJavaObject(index)!!
        else -> throwTypeError(index, clazz, type)
    }
    if (clazz != LuaReference::class && value is LuaReference<*, *>) {
        value = value.resolve() ?: throwError("Reference $value is invalid")
    }
    if (!clazz.isInstance(value)) {
        throwTypeError(index, clazz, value::class)
    }
    @Suppress("UNCHECKED_CAST")
    return value as T
}

inline fun <reified T : Any> Lua.toUserdata(index: Int): T? {
    return toUserdata(index, T::class)
}

fun <T : Any> Lua.toUserdata(index: Int, clazz: KClass<out T>): T? {
    if (top < abs(index)) {
        return null
    }
    val type = type(index)
    var value = when (type) {
        Lua.LuaType.USERDATA -> toJavaObject(index)!!
        else -> null
    }
    if (clazz != LuaReference::class && value is LuaReference<*, *>) {
        value = value.resolve() ?: throwError("Reference $value is invalid")
    }
    if (!clazz.isInstance(value)) {
        return null
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
    return when (val type = type(index)) {
        Lua.LuaType.USERDATA -> {
            return checkUserdata(index, clazz)
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

fun Lua.checkDirection(index: Int, grid: Grid): Grid.Direction {
    return if (type(index) == Lua.LuaType.STRING) {
        grid.getDirectionByName(toString(index)!!) ?: throwTypeError(
            index,
            Grid.Direction::class,
            Lua.LuaType.STRING
        )
    } else if (type(index) == Lua.LuaType.USERDATA) {
        checkUserdata(index, Grid.Direction::class)
    } else {
        throwTypeError(index, Grid.Direction::class, type(index))
    }
}

fun Lua.checkCoordinate(index: Int): Pair<Coordinate, Int> {
    if (type(index) == Lua.LuaType.TABLE) {
        val x = getFieldInt(index, "x") ?: 0
        val y = getFieldInt(index, "y") ?: 0
        val z = getFieldInt(index, "z") ?: 0
        return Pair(Coordinate(x, y, z), index)
    } else if (type(index) == Lua.LuaType.USERDATA) {
        return Pair(checkUserdata(index, Coordinate::class), index)
    } else {
        return Pair(Coordinate(checkInt(index), checkInt(index + 1), checkInt(index + 2)), index + 2)
    }
}

fun Lua.toFunction(index: Int): LuaValue? {
    if (type(index) == Lua.LuaType.FUNCTION) {
        pushValue(index)
        return get()
    }
    return null
}

fun Lua.checkFunction(index: Int): LuaValue {
    return toFunction(index) ?: throwTypeError(index, Lua.LuaType.FUNCTION)
}

fun <T : Any> Lua.toRegistry(index: Int, registry: Registry<T>): T? {
    if (isUserdata(index)) {
        return toUserdata(index, registry.clazz)
    }
    return toString(index)?.let { registry.get(it) }
}

fun <T : Any> Lua.checkRegistry(index: Int, registry: Registry<T>): T {
    if (isUserdata(index)) {
        return checkUserdata(index, registry.clazz)
    }
    return toRegistry(index, registry) ?: throwTypeError(index, Registry::class)
}

fun Lua.toAny(index: Int): Any? {
    return when (type(index)) {
        Lua.LuaType.STRING -> return toString(index)!!
        Lua.LuaType.NUMBER -> return toNumber(index).let { if (it % 1.0 == 0.0) it.toInt() else it }
        Lua.LuaType.BOOLEAN -> return toBoolean(index)
        Lua.LuaType.TABLE -> return toAnyMap(index)
        Lua.LuaType.FUNCTION -> return toFunction(index)
        Lua.LuaType.USERDATA -> return toJavaObject(index)
        else -> null
    }
}

fun Lua.toManagedTable(index: Int): ManagedLuaTable? {
    return when (type(index)) {
        Lua.LuaType.TABLE -> {
            return ManagedLuaTable(toAnyMap(index) as MutableMap)
        }

        Lua.LuaType.USERDATA -> return toUserdata(index, ManagedLuaTable::class)

        else -> null
    }
}

fun Lua.toAnyMap(index: Int): Map<Any, Any>? {
    if (isTable(index)) {
        val map = mutableMapOf<Any, Any>()
        val absIndex = (this as AbstractLua).toAbsoluteIndex(index)
        pushNil()
        while (next(absIndex) != 0) {
            val key = toAny(-2)!!
            val value = toAny(-1)!!
            map[key] = value
            pop(1)
        }
        return map
    } else if (isUserdata(index)) {
        return toUserdata(index, ManagedLuaTable::class)?.map
    }

    return null
}

fun Lua.checkAnyMap(index: Int): Map<Any, Any> {
    return toAnyMap(index) ?: throwTypeError(index, Lua.LuaType.TABLE)
}

inline fun <reified TKey : Any, reified TValue : Any> Lua.toTypedMap(index: Int): Map<TKey, TValue>? {
    return toTypedMap(index, TKey::class, TValue::class)
}

fun <TKey : Any, TValue : Any> Lua.toTypedMap(
    index: Int,
    keyClass: KClass<TKey>,
    valueClass: KClass<TValue>
): Map<TKey, TValue>? {
    if (isTable(index)) {
        val map = mutableMapOf<TKey, TValue>()
        val absIndex = (this as AbstractLua).toAbsoluteIndex(index)
        pushNil()
        while (next(absIndex) != 0) {
            val key = toAny(-2)!!
            if (!keyClass.isInstance(key)) {
                throwError("Expected only keys of type ${keyClass.simpleName} in table")
            }
            val value = toAny(-1)!!
            if (!valueClass.isInstance(value)) {
                throwError("Expected only values of type ${valueClass.simpleName} in table")
            }
            @Suppress("UNCHECKED_CAST")
            map[key as TKey] = value as TValue
            pop(1)
        }
        return map
    }

    return null
}

fun Lua.toLocale(index: Int): Locale? {
    return when (type(index)) {
        Lua.LuaType.STRING -> Locale.forLanguageTag(toString(index)!!)
        Lua.LuaType.USERDATA -> toUserdata(index, Locale::class)
        else -> throwTypeError(index, Locale::class)
    }
}

fun Lua.checkLocale(index: Int): Locale {
    return toLocale(index) ?: throwTypeError(index, Locale::class)
}

fun Lua.xpCall(nArgs: Int, nResults: Int, trace: LuaTrace? = null) {
    val base = top - nArgs
    pushErrorHandler()
    insert(base)
    xpCallWithErrorHandler(nArgs, nResults, base, trace)
}

fun Lua.xpCallWithErrorHandler(nArgs: Int, nResults: Int, errorHandler: Int, trace: LuaTrace? = null) {
    checkStack((nResults - nArgs - 1).coerceAtLeast(0));
    val code = luaNatives.lua_pcall(pointer, nArgs, nResults, errorHandler)
    val error = (this as AbstractLua).convertError(code)
    if (error == LuaError.OK) {
        return
    }

    val message: String
    if (type(-1) === LuaType.STRING) {
        message = toString(-1)!! + if (trace != null) "\n\t${trace.luaTrace()}" else ""
        pop(1)
    } else {
        message = "no error message available"
    }
    val e = LuaException(error, message.replace(Regex("\\[string \"(.*?)\"]"), "[$1]"))
    val javaError = getJavaError()
    if (javaError != null) {
        e.initCause(javaError)
        error(null as Throwable?)
    }
    throw e
}

fun handleError(lua: Lua): Int {
    val message = lua.toString(1)
    lua.getGlobal("debug")
    lua.getField(-1, "traceback")
    lua.pCall(0, 1)
    val traceback = lua.toString(-1)
    lua.pop(1)
    lua.push("$message\n$traceback")
    return 1
}

fun Lua.pushErrorHandler(): Int {
    push(::handleError)
    return top
}
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
import world.selene.common.grid.Direction
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
    return error(LuaException(LuaError.RUNTIME, "$callerInfo: $message"))
}

fun Lua.throwError(message: String): Nothing {
    val callerInfo = getCallerInfo()
    throw LuaException(LuaError.RUNTIME, "$callerInfo: $message")
}

fun Lua.throwTypeError(index: Int, expectedType: LuaType, actualType: LuaType? = null): Nothing {
    val actualType = actualType ?: type(index)
    throwError("Expected $expectedType, got $actualType at index $index")
}

fun Lua.throwTypeError(index: Int, expectedType: KClass<*>, actualType: LuaType? = null): Nothing {
    val actualType = actualType ?: type(index)
    throwError("Expected ${expectedType.simpleName}, got $actualType at index $index")
}

fun Lua.throwTypeError(index: Int, expectedType: KClass<*>, actualType: KClass<*>): Nothing {
    throwError("Expected ${expectedType.simpleName}, got ${actualType.simpleName} at index $index")
}

fun Lua.checkBoolean(index: Int): Boolean {
    if (top < abs(index)) {
        throwTypeError(index, LuaType.BOOLEAN, LuaType.NIL)
    }
    return when (val type = type(index)) {
        LuaType.BOOLEAN -> toBoolean(index)
        else -> throwTypeError(index, LuaType.BOOLEAN, type)
    }
}

fun Lua.checkString(index: Int): String {
    if (top < abs(index)) {
        throwTypeError(index, LuaType.STRING, LuaType.NIL)
    }
    return when (val type = type(index)) {
        LuaType.STRING -> toString(index)!!
        else -> throwTypeError(index, LuaType.STRING, type)
    }
}

fun Lua.checkInt(index: Int): Int {
    if (top < abs(index)) {
        throwTypeError(index, LuaType.NUMBER, LuaType.NIL)
    }
    return when (val type = type(index)) {
        LuaType.NUMBER -> toInteger(index).toInt()
        else -> throwTypeError(index, LuaType.NUMBER, type)
    }
}

fun Lua.checkFloat(index: Int): Float {
    if (top < abs(index)) {
        throwTypeError(index, LuaType.NUMBER, LuaType.NIL)
    }
    return when (val type = type(index)) {
        LuaType.NUMBER -> toNumber(index).toFloat()
        else -> throwTypeError(index, LuaType.NUMBER, type)
    }
}

inline fun <reified T : Any> Lua.checkUserdata(index: Int): T {
    return checkUserdata(index, T::class)
}

fun <T : Any> Lua.checkUserdata(index: Int, clazz: KClass<out T>): T {
    if (top < abs(index)) {
        throwTypeError(index, clazz, LuaType.NIL)
    }
    var value = when (val type = type(index)) {
        LuaType.USERDATA -> toJavaObject(index)!!
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
        LuaType.USERDATA -> toJavaObject(index)!!
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
        throwTypeError(index, LuaType.STRING, LuaType.NIL)
    }
    return when (val type = type(index)) {
        LuaType.USERDATA -> {
            return checkUserdata(index, clazz)
        }

        LuaType.STRING -> {
            try {
                enumValueOf(toString(index)!!, clazz)
            } catch (_: IllegalArgumentException) {
                throwTypeError(index, clazz, type)
            }
        }

        else -> throwTypeError(index, LuaType.STRING, type)
    }
}

private fun <T : Enum<T>> enumValueOf(value: String, clazz: KClass<T>): T {
    return clazz.java.enumConstants.firstOrNull { it.name.equals(value, ignoreCase = true) } as T
}

fun Lua.checkType(index: Int, expectedType: LuaType) {
    if (top < abs(index)) {
        throwTypeError(index, expectedType, LuaType.NIL)
    }
    val type = type(index)
    if (type != expectedType) {
        throwTypeError(index, expectedType, type)
    }
}

fun Lua.checkDirection(index: Int, grid: Grid): Direction {
    return if (type(index) == LuaType.STRING) {
        grid.getDirectionByName(toString(index)!!) ?: throwTypeError(
            index,
            Direction::class,
            LuaType.STRING
        )
    } else if (type(index) == LuaType.USERDATA) {
        checkUserdata(index, Direction::class)
    } else {
        throwTypeError(index, Direction::class, type(index))
    }
}

fun Lua.checkCoordinate(index: Int): Pair<Coordinate, Int> {
    if (type(index) == LuaType.TABLE) {
        val x = getFieldInt(index, "x") ?: 0
        val y = getFieldInt(index, "y") ?: 0
        val z = getFieldInt(index, "z") ?: 0
        return Pair(Coordinate(x, y, z), index)
    } else if (type(index) == LuaType.USERDATA) {
        return Pair(checkUserdata<Coordinate>(index), index)
    } else {
        return Pair(Coordinate(checkInt(index), checkInt(index + 1), checkInt(index + 2)), index + 2)
    }
}

fun Lua.toFunction(index: Int): LuaValue? {
    if (type(index) == LuaType.FUNCTION) {
        pushValue(index)
        return get()
    }
    return null
}

fun Lua.checkFunction(index: Int): LuaValue {
    return toFunction(index) ?: throwTypeError(index, LuaType.FUNCTION)
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
        LuaType.STRING -> return toString(index)!!
        LuaType.NUMBER -> return toNumber(index).let { if (it % 1.0 == 0.0) it.toInt() else it }
        LuaType.BOOLEAN -> return toBoolean(index)
        LuaType.TABLE -> return toAnyMap(index)
        LuaType.FUNCTION -> return toFunction(index)
        LuaType.USERDATA -> return toJavaObject(index)
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
        return toUserdata(index, ObservableMap::class)?.map
    }

    return null
}

fun Lua.checkAnyMap(index: Int): Map<Any, Any> {
    return toAnyMap(index) ?: throwTypeError(index, LuaType.TABLE)
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
        LuaType.STRING -> Locale.forLanguageTag(toString(index)!!)
        LuaType.USERDATA -> toUserdata(index, Locale::class)
        else -> throwTypeError(index, Locale::class)
    }
}

fun Lua.xpCall(nArgs: Int, nResults: Int, trace: LuaTrace? = null) {
    val base = top - nArgs
    pushErrorHandler()
    insert(base)
    xpCallWithErrorHandler(nArgs, nResults, base, trace)
}

fun Lua.xpCallWithErrorHandler(nArgs: Int, nResults: Int, errorHandler: Int, trace: LuaTrace? = null) {
    checkStack((nResults - nArgs - 1).coerceAtLeast(0))
    val code = luaNatives.lua_pcall(pointer, nArgs, nResults, errorHandler)
    remove(errorHandler)
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
    val e = LuaException(
        error, message
            .replace(Regex("\\[string \"(.*?)\"]"), "[$1]")
            .replace("\t[C]: in ?\n", "")
            .replace("[C]", "[Selene]")
    )
    val javaCause = getJavaCause()
    if (javaCause != null) {
        e.initCause(javaCause)
        error(null as Throwable?)
        pushNil().also { setGlobal(JAVA_CAUSE) }
    }
    throw e
}

const val JAVA_CAUSE = "__jcause__"

private fun Lua.getJavaCause(): Throwable? {
    return getGlobal(JAVA_CAUSE).let { toJavaObject(-1) }.also { pop(1) } as? Throwable
}

@Suppress("SameReturnValue")
fun handleError(lua: Lua): Int {
    val message = lua.toString(1)
    lua.getGlobal("debug")
    lua.getField(-1, "traceback")
    lua.pCall(0, 1)
    val traceback = lua.toString(-1)
    lua.pop(1)
    lua.push("$message\n$traceback")
    // LuaNatives are resetting the global throwable because the error handler completes successfully lol
    lua.getGlobal(Lua.GLOBAL_THROWABLE)
    lua.setGlobal(JAVA_CAUSE)
    return 1
}

fun Lua.pushErrorHandler(): Int {
    push(::handleError)
    return top
}
package world.selene.common.lua

import org.koin.mp.KoinPlatform.getKoin
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.lua54.Lua54
import party.iroiro.luajava.value.LuaValue
import world.selene.common.bundles.LocatedBundle
import world.selene.common.grid.Grid
import world.selene.common.util.Coordinate
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

class LuaManager(private val mixinRegistry: LuaMixinRegistry) {

    private val logger = LoggerFactory.getLogger("selene.lua")

    val lua = Lua54()
    private val packages = mutableMapOf<String, LuaValue>()
    private val exposedClasses = mutableSetOf(
        LuaSignal::class,
        Coordinate::class,
        Grid.Direction::class
    )
    private val packageResolvers = mutableListOf<(Lua, String) -> LuaValue?>()

    init {
        secureClassMetatable()
        secureObjectMetatable()

        lua.openLibrary("os")
        val os = lua.get("os")
        val luaOsDate = os.get("date")
        val luaOsTime = os.get("time")
        val luaClock = os.get("clock")
        lua.set("os", lua.newTable {
            set("date", luaOsDate)
            set("time", luaOsTime)
            set("clock", luaClock)
        })

        lua.openLibrary("string")
        packages["string"] = lua.get("string").also {
            it.register("trim", this::luaTrim)
            it.register("startsWith", this::luaStartsWith)
        }
        lua.openLibrary("bit32")
        packages["bit32"] = lua.get("bit32")
        lua.openLibrary("math")
        packages["math"] = lua.get("math")
        lua.openLibrary("table")
        packages["table"] = lua.get("table").also {
            it.register("find", this::luaTableFind)
        }

        lua.register("require", this::luaRequire)
        lua.register("print2", this::luaPrint)

        lua.set("dofile", null)
        lua.set("loadfile", null)
        lua.set("load", null)
        lua.set("loadstring", null)
        lua.set("setfenv", null)
        lua.set("getfenv", null)
        lua.set("collectgarbage", null)
        lua.set("module", null)
        lua.set("_G", null)
    }

    fun loadModules() {
        val modules = getKoin().getAll<LuaModule>()
        for (module in modules) {
            module.initialize(this)
            packages[module.name] = lua.newTable {
                module.register(this)
            }
        }
    }

    fun exposeClass(clazz: KClass<*>) {
        exposedClasses.add(clazz)
    }

    fun addPackageResolver(resolver: (Lua, String) -> LuaValue?) {
        packageResolvers.add(resolver)
    }

    private fun secureClassMetatable() {
        lua.getRegisteredMetatable("__jclass__") // [metatable]
        lua.pushNil() // [metatable, nil]
        lua.setField(-2, "__index") // [metatable]
        lua.pushNil() // [metatable, nil]
        lua.setField(-2, "__newindex") // [metatable]
        lua.pushNil() // [metatable, nil]
        lua.setField(-2, "__call") // [metatable]
        lua.pop(1)
    }

    private fun secureObjectMetatable() {
        lua.getRegisteredMetatable("__jobject__") // [metatable]
        lua.getField(-1, "__index") // [metatable, index]
        val nativeIndex = lua.get() // [metatable]
        lua.push { lua ->
            val obj = lua.toJavaObject(-2)!! // [key, obj]
            val key = lua.toString(-1)!!
            lastAccessedMember = "${obj::class.simpleName}.$key"
            if (!exposedClasses.contains(obj::class)) {
                logger.warn("Tried to access restricted class: ${obj::class}#${key}")
                lua.pushNil() // [key, obj, nil]
                return@push 1
            }
            val function = obj::class.simpleName?.let { mixinRegistry.getMixin(it, lua.toString(-1)!!) }
            if (function != null) {
                lua.push(function)
                return@push 1
            }
            // TODO Would be nice to have a cache of this. Maybe proxies can extend a superclass to provide it.
            val property = obj::class.memberProperties.find { it.name == key } as? KProperty1<Any, *>
            if (property != null) {
                val value = property.get(obj)
                lua.push(value, Lua.Conversion.FULL)
                return@push 1
            }
            val luaMethod = obj::class.memberFunctions.find { it.name == key }
            if (luaMethod != null && luaMethod.returnType == typeOf<Int>() && luaMethod.parameters.size == 2 &&
                luaMethod.parameters[1].type == typeOf<Lua>()
            ) {
                lua.push { lua -> luaMethod.call(obj, lua) as Int }
                return@push 1
            }
            lua.push(nativeIndex) // [key, obj, index]
            lua.pushValue(-3) // [key, obj, index, key]
            lua.pushValue(-3) // [key, obj, index, key, obj]
            lua.pCall(2, 1) // [key, obj, result]
            1
        } // [metatable, function]
        lua.setField(-2, "__index") // [metatable]

        lua.getField(-1, "__newindex") // [metatable, index]
        val nativeNewIndex = lua.get() // [metatable]
        lua.push { lua ->
            val obj = lua.toJavaObject(-3)!! // [obj, key, value]
            if (!exposedClasses.contains(obj::class)) {
                return@push lua.error(
                    IllegalArgumentException(
                        "Tried to access restricted class: ${obj::class}#${
                            lua.checkString(
                                -2
                            )
                        }"
                    )
                )
            }

            val key = lua.checkString(-2)
            val property = obj::class.memberProperties.find { it.name == key } as? KMutableProperty1<Any, Any?>
            if (property != null) {
                val value = when (property.returnType.classifier) {
                    Boolean::class -> lua.checkBoolean(-1)
                    Int::class -> lua.checkInt(-1)
                    String::class -> lua.checkString(-1)
                    Float::class -> lua.checkFloat(-1)
                    else -> lua.toJavaObject(-1)
                }
                property.set(obj, value)
                return@push 0
            }
            lua.push(nativeNewIndex) // [obj, key, value, index]
            lua.pushValue(-4) // [obj, key, value, index, key]
            lua.pushValue(-4) // [obj, key, value, index, key, obj]
            lua.pushValue(-4) // [obj, key, value, index, key, obj, value]
            lua.pCall(3, 0) // [obj, key, value]
            0
        } // [metatable, function]
        lua.setField(-2, "__newindex") // [metatable]

        lua.push {
            val obj = it.toJavaObject(-1)!! // [obj]
            if (!exposedClasses.contains(obj::class)) {
                it.push(obj::class.simpleName ?: "Object") // [obj, name]
                return@push 1
            }
            it.push(obj.toString())
            1
        }
        lua.setField(-2, "__tostring")

        lua.push {
            val first = it.toJavaObject(1)!! // [obj, other]
            val second = it.toJavaObject(2)!! // [obj, other]
            it.push(first == second)
            1
        }
        lua.setField(-2, "__eq")

        lua.pop(1) // []
    }

    private fun luaStartsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).startsWith(lua.checkString(2)))
        return 1
    }

    private fun luaTrim(lua: Lua): Int {
        lua.push(lua.checkString(1).trim())
        return 1
    }

    private fun luaTableFind(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        lua.top = 2

        var idx = 1
        lua.pushNil() // initial key for next call

        // Stack: table(1), target(2), nil(3)
        while (lua.next(1) != 0) { // pushes key-value pair
            // Stack: table(1), target(2), key(3), value(4)

            // Compare the value with target
            if (lua.equal(4, 2)) {
                lua.pushValue(3) // push the key as return value
                return 1
            }

            lua.pop(1) // pop value, keep key for next iteration
            // Stack: table(1), target(2), key(3)
            idx++
        }

        // Return nil if not found
        lua.pushNil()
        return 1
    }

    private fun luaPrint(lua: Lua, args: Array<LuaValue>): Array<LuaValue> {
        logger.info(args.joinToString(" ") { it.toString() })
        return emptyArray()
    }

    private fun luaRequire(lua: Lua, args: Array<LuaValue>): Array<LuaValue> {
        val path = args.getOrNull(0)?.toString()
        if (path == null) {
            throw IllegalArgumentException("require path must be a string")
        }
        return arrayOf(requireModule(path))
    }

    fun requireModule(moduleName: String): LuaValue {
        val module = packages.getOrPut(moduleName) {
            val module = packageResolvers.asSequence().mapNotNull { it(lua, moduleName) }.firstOrNull()
            if (module == null) {
                throw IllegalArgumentException("Failed to load module: $moduleName")
            }
            module
        }
        if (module.type() == Lua.LuaType.FUNCTION) {
            return module.call()[0]
        }
        return module
    }

    private fun loadBuffer(script: String): Buffer {
        val bytes = script.toByteArray()
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    fun preloadModule(moduleName: String, script: String) {
        if (packages.containsKey(moduleName)) {
            logger.warn("Module $moduleName already loaded, skipping preload")
            return
        }

        lua.load(loadBuffer(script), moduleName)
        packages[moduleName] = lua.get()
    }

    fun runScript(bundle: LocatedBundle, file: File, script: String) {
        return runScript(bundle.manifest.name + ":" + file.name, script)
    }

    private fun runScript(name: String, script: String) {
        try {
            lua.run(loadBuffer(script), name)
        } catch (e: LuaException) {
            throw sanitizeException(e)
        }
    }

    fun setGlobal(key: String, value: Any) {
        lua.set(key, value)
    }

    var lastAccessedMember: String? = null; private set

    fun sanitizeException(e: Exception): Exception {
        if (e.message == "no matching method found") {
            return LuaException(LuaException.LuaError.JAVA, "${e.message} ($lastAccessedMember)")
        }
        return e
    }
}
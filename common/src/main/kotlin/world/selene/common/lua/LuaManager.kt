package world.selene.common.lua

import org.koin.mp.KoinPlatform.getKoin
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.lua54.Lua54
import party.iroiro.luajava.value.LuaValue
import world.selene.common.bundles.LocatedBundle
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class LuaManager(private val mixinRegistry: LuaMixinRegistry) {

    private val logger = LoggerFactory.getLogger("selene.lua")

    val lua = Lua54()
    private val packages = mutableMapOf<String, LuaValue>()
    private val metatables = mutableMapOf<KClass<*>, LuaMetatable>()
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
            it.register("endsWith", this::luaEndsWith)
            it.register("split", this::luaSplit)
        }

        lua.openLibrary("bit32")
        packages["bit32"] = lua.get("bit32")

        lua.openLibrary("math")
        packages["math"] = lua.get("math")

        lua.openLibrary("debug")
        debugLibrary = lua.get("debug")
        lua.set("debug", lua.newTable {
            this.register("getinfo", this@LuaManager::luaDebugGetInfo)
            this.register("traceback", this@LuaManager::luaTraceback)
        })

        lua.openLibrary("table")
        packages["table"] = lua.get("table").also {
            it.register("find", this::luaTableFind)
            it.register("tostring", this::luaTableToString)
        }

        lua.register("require", this::luaRequire)

        lua.set("dofile", null)
        lua.set("loadfile", null)
        lua.set("load", null)
        lua.set("loadstring", null)
        lua.set("setfenv", null)
        lua.set("getfenv", null)
        lua.set("collectgarbage", null)
        lua.set("module", null)
        lua.set("java", null)
        lua.set("_G", lua.newTable {})
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

    fun defineMetatable(clazz: KClass<*>, metatable: LuaMetatable) {
        metatables[clazz] = metatable
    }

    fun addPackageResolver(resolver: (Lua, String) -> LuaValue?) {
        packageResolvers.add(resolver)
    }

    private fun secureClassMetatable() {
        lua.getRegisteredMetatable("__jclass__")
        lua.pushNil()
        lua.setField(-2, "__index")
        lua.pushNil()
        lua.setField(-2, "__newindex")
        lua.pushNil()
        lua.setField(-2, "__call")
        lua.pop(1)
    }

    private fun secureObjectMetatable() {
        lua.getRegisteredMetatable("__jobject__")
        lua.push { lua ->
            val obj = lua.toJavaObject(1)!!
            val mixinFunction = obj::class.simpleName?.let { mixinRegistry.getMixin(it, lua.toString(2)!!) }
            if (mixinFunction != null) {
                lua.push(mixinFunction)
                return@push 1
            }

            if (obj is LuaMetatable) {
                return@push obj.luaGet(lua)
            } else if (obj is LuaMetatableProvider) {
                return@push obj.luaMetatable(lua).luaGet(lua)
            }
            val metatable = metatables[obj::class]
            if (metatable != null) {
                return@push metatable.luaGet(lua)
            }

            lua.pushNil()
            return@push 1
        }
        lua.setField(-2, "__index")

        lua.push { lua ->
            val obj = lua.toJavaObject(-3)!!
            if (obj is LuaMetatable) {
                return@push obj.luaSet(lua)
            } else if (obj is LuaMetatableProvider) {
                return@push obj.luaMetatable(lua).luaSet(lua)
            }
            val metatable = metatables[obj::class]
            if (metatable != null) {
                return@push metatable.luaSet(lua)
            }

            val key = lua.toString(-1)!!
            return@push lua.error(NoSuchFieldException("Property '$key' does not exist on ${obj::class.simpleName ?: "(anonymous class)"}"))
        }
        lua.setField(-2, "__newindex")

        lua.push {
            val obj = it.toJavaObject(-1)!!
            if (obj is LuaMetatable) {
                it.push(obj.luaToString(it))
                return@push 1
            } else if (obj is LuaMetatableProvider) {
                it.push(obj.luaMetatable(it).luaToString(it))
                return@push 1
            }
            val metatable = metatables[obj::class]
            if (metatable != null) {
                it.push(metatable.luaToString(it))
                return@push 1
            }

            it.push(obj::class.simpleName ?: "(anonymous class)")
            return@push 1
        }
        lua.setField(-2, "__tostring")

        lua.push {
            val obj = it.toJavaObject(1)!!
            if (obj is LuaMetatable) {
                return@push obj.luaCall(lua)
            } else if (obj is LuaMetatableProvider) {
                return@push obj.luaMetatable(lua).luaCall(lua)
            }
            val metatable = metatables[obj::class]
            if (metatable != null) {
                return@push metatable.luaCall(lua)
            }

            return@push lua.error(NoSuchFieldException("Object of type ${obj::class.simpleName ?: "(anonymous class)"} cannot be called"))
        }
        lua.setField(-2, "__call")

        lua.push {
            val first = it.toJavaObject(1)!!
            if (first is LuaMetatable) {
                it.push(first.luaEquals(it))
                return@push 1
            } else if (first is LuaMetatableProvider) {
                it.push(first.luaMetatable(it).luaEquals(it))
                return@push 1
            }
            val metatable = metatables[first::class]
            if (metatable != null) {
                it.push(metatable.luaEquals(it))
                return@push 1
            }

            val second = it.toJavaObject(2)!!
            if (second is LuaMetatable) {
                it.push(second.luaEquals(it))
                return@push 1
            } else if (second is LuaMetatableProvider) {
                it.push(second.luaMetatable(it).luaEquals(it))
                return@push 1
            }
            val secondMetatable = metatables[second::class]
            if (secondMetatable != null) {
                it.push(secondMetatable.luaEquals(it))
                return@push 1
            }

            it.push(first == second)
            1
        }
        lua.setField(-2, "__eq")

        lua.pop(1)
    }

    private fun luaStartsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).startsWith(lua.checkString(2)))
        return 1
    }

    private fun luaEndsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).endsWith(lua.checkString(2)))
        return 1
    }

    private fun luaTableToString(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)

        lua.pushNil()
        val sb = StringBuilder("{")
        while (lua.next(-2) != 0) {
            if (sb.length > 1) {
                sb.append(", ")
            }

            lua.getGlobal("tostring")
            lua.pushValue(-3)
            lua.pCall(1, 1)
            val key = lua.toString(-1).also { lua.pop(1) }

            lua.getGlobal("tostring")
            lua.pushValue(-2)
            lua.pCall(1, 1)
            val value = lua.toString(-1).also { lua.pop(1) }

            sb.append(key)
            sb.append(" = ")
            sb.append(value)
            lua.pop(1)
        }
        sb.append("}")
        lua.push(sb.toString())
        return 1
    }

    private fun luaTrim(lua: Lua): Int {
        lua.push(lua.checkString(1).trim())
        return 1
    }

    private fun luaSplit(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.split(separator)
        lua.push(result, Lua.Conversion.FULL)
        return 1
    }

    private fun luaDebugGetInfo(lua: Lua): Int {
        val offset = lua.checkInt(1)

        val callerInfo = lua.getCallerInfo(offset)
        lua.newTable()
        lua.push(callerInfo.source)
        lua.setField(-2, "short_src")
        lua.push(callerInfo.line)
        lua.setField(-2, "currentline")
        return 1
    }

    private fun luaTraceback(lua: Lua): Int {
        debugLibrary.push(lua)
        lua.getField(-1, "traceback")
        lua.pCall(0, 1)
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
        lua.run(loadBuffer(script), name)
    }

    fun setGlobal(key: String, value: Any) {
        lua.set(key, value)
    }

    companion object {
        lateinit var debugLibrary: LuaValue
    }
}
package world.selene.common.lua

import org.koin.mp.KoinPlatform.getKoin
import party.iroiro.luajava.ClassPathLoader
import party.iroiro.luajava.lua54.Lua54
import world.selene.common.lua.libraries.LuaPackageModule
import world.selene.common.lua.util.newTable
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class LuaManager(private val luaPackage: LuaPackageModule) {

    val lua = Lua54()
    private val metatables = mutableMapOf<KClass<*>, LuaMetatable>()

    init {
        // Default metatables of LuaJava are unsafe, we override them. Adds support for LuaMetatable interface too.
        secureClassMetatable()
        secureObjectMetatable()

        luaPackage.initializeEarly(lua)

        // bit32 was removed from this version of Lua, we provide a polyfill from classpath
        lua.setExternalLoader(ClassPathLoader())
        lua.push(luaPackage.packageLoaded)
        lua.loadExternal("bit32")
        lua.pCall(0, 1)
        lua.pushValue(-1)
        lua.setGlobal("bit32")
        lua.setField(-2, "bit32")
        lua.pop(1)

        // Load standard libraries, but only those that are safe
        val libraries = setOf("string", "math", "table")
        lua.push(luaPackage.packageLoaded)
        libraries.forEach {
            lua.openLibrary(it)
            lua.getGlobal(it)
            lua.setField(-2, it)
        }
        lua.pop(1)

        // Remove some globals that shouldn't be used in Lua scripts
        val bannedGlobals =
            setOf("dofile", "loadfile", "load", "loadstring", "setfenv", "getfenv", "collectgarbage", "module", "java")
        bannedGlobals.forEach { lua.set(it, null) }

        // Reset _G to make sure it doesn't leak access to anything we don't want
        lua.newTable()
        lua.setGlobal("_G")
    }

    fun loadModules() {
        val modules = getKoin().getAll<LuaModule>()
        for (module in modules) {
            module.initialize(this)
            lua.push(luaPackage.packageLoaded)
            lua.push(lua.newTable {
                module.register(this)
            })
            if (module.registerAsGlobal) {
                lua.pushValue(-1)
                lua.setGlobal(module.name)
            }
            lua.setField(-2, module.name)
            lua.pop(1)
        }
    }

    fun defineMetatable(clazz: KClass<*>, metatable: LuaMetatable) {
        metatables[clazz] = metatable
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

    companion object {
        fun loadBuffer(script: String): Buffer {
            val bytes = script.toByteArray()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            buffer.flip()
            return buffer
        }
    }
}
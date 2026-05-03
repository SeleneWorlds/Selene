package com.seleneworlds.common.lua

import org.koin.mp.KoinPlatform.getKoin
import party.iroiro.luajava.ClassPathLoader
import party.iroiro.luajava.Lua
import party.iroiro.luajava.lua54.Lua54
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.IdentifierLuaApi
import com.seleneworlds.common.data.RegistryObject
import com.seleneworlds.common.data.RegistryObjectLuaApi
import com.seleneworlds.common.data.custom.CustomRegistryObject
import com.seleneworlds.common.data.custom.CustomRegistryObjectLuaApi
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.CoordinateLuaApi
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.observable.ObservableMapLuaApi
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.common.util.ResolvableReferenceLuaApi
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class LuaManager(private val luaPackage: LuaPackageModule) {

    val lua = Lua54()

    init {
        defineMetatable(Coordinate::class, CoordinateLuaApi.luaMeta)
        defineMetatable(Identifier::class, IdentifierLuaApi.luaMeta)
        defineMetatable(RegistryObject::class, RegistryObjectLuaApi.luaMeta)
        defineMetatable(CustomRegistryObject::class, CustomRegistryObjectLuaApi.luaMeta)
        defineMetatable(LuaEventSink::class, LuaEventSink.luaMeta)
        defineMetatable(ResolvableReference::class, ResolvableReferenceLuaApi.luaMeta)
        defineMetatable(ObservableMap::class, ObservableMapLuaApi.luaMeta)

        // Default metatables of LuaJava are unsafe, we override them. Adds support for LuaMetatable interface too.
        secureClassMetatable()
        secureObjectMetatable()

        luaPackage.initializeEarly(lua)
        lua.setExternalLoader(ClassPathLoader())
        loadInternalLuaModule("bit32")

        // Load standard libraries, but only those that are safe
        val libraries = setOf("string", "math", "table", "coroutine")
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

    fun loadInternalLuaModule(module: String) {
        // bit32 was removed from this version of Lua, we provide a polyfill from classpath
        lua.push(luaPackage.packageLoaded)
        lua.loadExternal(module)
        lua.pCall(0, 1)
        lua.pushValue(-1)
        lua.setGlobal(module)
        lua.setField(-2, module)
        lua.pop(1)
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
        registerMetatable(clazz, metatable)
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
            val metatable = findMetatable(lua, obj)
            if (metatable != null) {
                return@push metatable.luaGet(lua)
            }

            lua.pushNil()
            return@push 1
        }
        lua.setField(-2, "__index")

        lua.push { lua ->
            val obj = lua.toJavaObject(-3)!!
            val metatable = findMetatable(lua, obj)
            if (metatable != null) {
                return@push metatable.luaSet(lua)
            }

            val key = lua.toString(-1)!!
            return@push lua.error(NoSuchFieldException("Property '$key' does not exist on ${obj::class.simpleName ?: "(anonymous class)"}"))
        }
        lua.setField(-2, "__newindex")

        lua.push {
            val obj = it.toJavaObject(-1)!!
            val metatable = findMetatable(it, obj)
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
            val metatable = findMetatable(it, obj)
            if (metatable != null) {
                return@push metatable.luaCall(lua)
            }

            return@push lua.error(NoSuchFieldException("Object of type ${obj::class.simpleName ?: "(anonymous class)"} cannot be called"))
        }
        lua.setField(-2, "__call")

        lua.push {
            val first = it.toJavaObject(1)!!
            val metatable = findMetatable(it, first)
            if (metatable != null) {
                it.push(metatable.luaEquals(it))
                return@push 1
            }

            val second = it.toJavaObject(2)!!
            val secondMetatable = findMetatable(it, second)
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
        private val metatables = mutableMapOf<KClass<*>, LuaMetatable>()

        fun registerMetatable(clazz: KClass<*>, metatable: LuaMetatable) {
            metatables[clazz] = metatable
        }

        fun findMetatable(lua: Lua, obj: Any): LuaMetatable? {
            if (obj is LuaMetatableProvider) {
                return obj.luaMetatable(lua)
            }
            if (obj is LuaMetatable) {
                return obj
            }
            return metatables[obj::class] ?: metatables.entries.firstOrNull { (clazz, _) -> clazz.isInstance(obj) }?.value
        }

        fun loadBuffer(script: String): Buffer {
            val bytes = script.toByteArray()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            buffer.flip()
            return buffer
        }
    }
}

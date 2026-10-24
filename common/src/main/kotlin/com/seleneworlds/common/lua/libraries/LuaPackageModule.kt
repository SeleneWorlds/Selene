package com.seleneworlds.common.lua.libraries

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import org.slf4j.LoggerFactory
import com.seleneworlds.common.lua.util.pushError
import com.seleneworlds.common.lua.util.register

/**
 * Access other bundles and lua modules.
 * Registered as `package` global.
 */
class LuaPackageModule : LuaModule {
    override val name: String = "package"

    private var executingScript: String? = null
    private val unloadCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()

    /** Register cleanup against the script currently being loaded, including nested requires. */
    fun onScriptUnload(callback: () -> Unit) {
        val script = checkNotNull(executingScript) { "Unload callbacks must be registered while loading a script" }
        unloadCallbacks.getOrPut(script) { mutableListOf() }.add(callback)
    }

    private fun unloadScript(script: String) {
        val callbacks = unloadCallbacks.remove(script) ?: return
        callbacks.asReversed().forEach { callback ->
            try {
                callback()
            } catch (exception: Exception) {
                LoggerFactory.getLogger(LuaPackageModule::class.java).error("Error cleaning up script {}", script, exception)
            }
        }
    }

    fun <T> withScript(script: String, block: () -> T): T {
        unloadScript(script)
        val previous = executingScript
        executingScript = script
        try {
            return block()
        } catch (exception: Exception) {
            unloadScript(script)
            throw exception
        } finally {
            executingScript = previous
        }
    }

    private val packageResolvers = mutableListOf<(String) -> Pair<String, String>?>()

    lateinit var packageLoaded: LuaValue
    lateinit var packagePreload: LuaValue

    fun initializeEarly(lua: Lua) {
        lua.newTable()
        lua.newTable()
        lua.pushValue(-1)
        packageLoaded = lua.get()
        lua.setField(-2, "loaded")
        lua.newTable()
        lua.pushValue(-1)
        packagePreload = lua.get()
        lua.setField(-2, "preload")
        lua.setGlobal("package")
    }

    fun addPackageResolver(resolver: (String) -> Pair<String, String>?) {
        packageResolvers.add(resolver)
    }

    fun preloadModule(lua: Lua, moduleName: String, script: String, sourceName: String = moduleName) {
        lua.push(packagePreload)
        lua.load(LuaManager.Companion.loadBuffer(script), sourceName)
        lua.setField(-2, moduleName)
        lua.pop(1)
    }

    fun clearLoadedModule(lua: Lua, moduleName: String) {
        unloadScript(moduleName)
        lua.push(packageLoaded)
        lua.pushNil()
        lua.setField(-2, moduleName)
        lua.pop(1)
    }

    fun removePreloadedModule(lua: Lua, moduleName: String) {
        lua.push(packagePreload)
        lua.pushNil()
        lua.setField(-2, moduleName)
        lua.pop(1)
    }

    override fun initialize(luaManager: LuaManager) {
        luaManager.lua.register("require", this::require)
    }

    override fun register(table: LuaValue) = Unit

    /**
     * Loads a module by name. Throws an error if the module cannot be found or failed to load.
     * As with Lua's standard `require` function, this will only load the module once and then cache it for future use.
     *
     * ```signatures
     * require(moduleName: string) -> any
     * ```
     */
    private fun require(lua: Lua): Int {
        val moduleName = lua.checkString(1)
        lua.push(packageLoaded)
        lua.getField(-1, moduleName)
        lua.remove(-2)
        if (!lua.isNil(-1)) {
            return 1
        }
        lua.pop(1)

        lua.push(packagePreload)
        lua.getField(-1, moduleName)
        if (!lua.isNil(-1)) {
            lua.remove(-2)
            withScript(moduleName) { lua.pCall(0, 1) }
            lua.push(packageLoaded)
            lua.pushValue(-2)
            lua.setField(-2, moduleName)
            lua.pop(1)
            return 1
        }
        lua.pop(2)

        val found = packageResolvers.asSequence().mapNotNull { it(moduleName) }.firstOrNull()
        if (found != null) {
            val preTop = lua.top
            lua.load(LuaManager.Companion.loadBuffer(found.second), found.first)
            withScript(moduleName) { lua.pCall(0, 1) }
            if (lua.top > preTop) {
                lua.push(packageLoaded)
                lua.pushValue(-2)
                lua.setField(-2, moduleName)
                lua.pop(1)
                return 1
            }
        }

        return lua.pushError("module '$moduleName' not found")
    }
}

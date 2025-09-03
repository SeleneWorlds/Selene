package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaPackageModule : LuaModule {
    override val name: String = "package"

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

    fun preloadModule(lua: Lua, moduleName: String, script: String) {
        lua.push(packagePreload)
        lua.load(LuaManager.loadBuffer(script), moduleName)
        lua.setField(-2, moduleName)
        lua.pop(1)
    }

    override fun initialize(luaManager: LuaManager) {
        luaManager.lua.register("require", this::luaRequire)
    }

    override fun register(table: LuaValue) = Unit

    private fun luaRequire(lua: Lua): Int {
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
            lua.pCall(0, 1)
            lua.push(packageLoaded)
            lua.pushValue(-2)
            lua.setField(-2, moduleName)
            lua.pop(1)
            return 1
        }
        lua.pop(1)

        val found = packageResolvers.asSequence().mapNotNull { it(moduleName) }.firstOrNull()
        if (found != null) {
            val preTop = lua.top
            lua.load(LuaManager.loadBuffer(found.second), found.first)
            lua.pCall(0, 1)
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
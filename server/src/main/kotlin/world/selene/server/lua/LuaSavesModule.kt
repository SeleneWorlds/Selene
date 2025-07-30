package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.server.config.ServerConfig
import world.selene.server.saves.SaveManager
import java.io.File

class LuaSavesModule(private val serverConfig: ServerConfig, private val saveManager: SaveManager) : LuaModule {
    override val name = "selene.saves"

    override fun register(table: LuaValue) {
        table.register("Has", this::luaHas)
        table.register("Save", this::luaSave)
        table.register("Load", this::luaLoad)
    }

    private fun luaHas(lua: Lua): Int {
        val path = lua.checkString(-1)
        val saveFile = File(serverConfig.savePath, path)
        lua.push(saveFile.exists())
        return 1
    }

    private fun luaSave(lua: Lua): Int {
        val savable = lua.toJavaObject(-2)
        val path = lua.checkString(-1)
        val saveFile = File(serverConfig.savePath, path)
        saveManager.save(saveFile, savable)
        return 0
    }

    private fun luaLoad(lua: Lua): Int {
        val path = lua.checkString(-1)
        val saveFile = File(serverConfig.savePath, path)
        lua.push(saveManager.load(saveFile)?.luaProxy, Lua.Conversion.FULL)
        return 1
    }

}
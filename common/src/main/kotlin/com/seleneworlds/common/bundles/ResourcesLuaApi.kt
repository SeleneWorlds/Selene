package com.seleneworlds.common.bundles

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register

/**
 * Lookup bundle resource files.
 */
class ResourcesLuaApi(private val api: ResourcesApi) : LuaModule {
    override val name = "selene.resources"

    override fun register(table: LuaValue) {
        table.register("listBundles", this::listBundles)
        table.register("listFiles", this::listFiles)
        table.register("loadAsString", this::loadAsString)
        table.register("saveAsString", this::saveAsString)
        table.register("createAsString", this::createAsString)
        table.register("fileExists", this::fileExists)
    }

    private fun listBundles(lua: Lua): Int {
        lua.push(api.listBundles(), Lua.Conversion.FULL)
        return 1
    }

    private fun listFiles(lua: Lua): Int {
        lua.push(api.listFiles(lua.checkString(1), lua.checkString(2)), Lua.Conversion.FULL)
        return 1
    }

    private fun loadAsString(lua: Lua): Int {
        lua.push(api.loadAsString(lua.checkString(1)))
        return 1
    }

    private fun saveAsString(lua: Lua): Int {
        api.saveAsString(lua.checkString(1), lua.checkString(2))
        return 0
    }

    private fun fileExists(lua: Lua): Int {
        lua.push(api.fileExists(lua.checkString(1)))
        return 1
    }

    private fun createAsString(lua: Lua): Int {
        api.createAsString(lua.checkString(1), lua.checkString(2))
        return 0
    }
}

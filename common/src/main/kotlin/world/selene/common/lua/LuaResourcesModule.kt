package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.bundles.BundleDatabase
import java.io.File

class LuaResourcesModule(private val bundleDatabase: BundleDatabase) : LuaModule {
    override val name = "selene.resources"

    override fun register(table: LuaValue) {
        table.register("ListFiles", this::luaListFiles)
        table.register("LoadAsString", this::luaLoadAsString)
    }

    private fun globToRegex(glob: String): Regex {
        return glob
            .replace("\\", "\\\\")
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex()
    }

    private fun luaListFiles(lua: Lua): Int {
        val bundle = lua.checkString(-2)
        val filter = lua.checkString(-1)
        val baseDir = bundleDatabase.getBundle(bundle)?.dir
        if (baseDir == null) {
            lua.newTable()
            return 1
        }

        val files = baseDir.walkTopDown().filter {
            it.isFile && it.relativeTo(baseDir).path.matches(globToRegex(filter))
        }.map {
            bundle + File.separator + it.relativeTo(baseDir).path.toString()
        }.toList()

        lua.newTable()
        for ((index, file) in files.withIndex()) {
            lua.push(index + 1)
            lua.push(file)
            lua.setTable(-3)
        }
        return 1
    }

    private fun luaLoadAsString(lua: Lua): Int {
        val path = lua.checkString(-1)
        val bundleName = path.substringBefore("/")
        val remainingPath = path.substringAfter("/")
        val baseDir = bundleDatabase.getBundle(bundleName)?.dir
        if (baseDir == null) {
            return lua.error(IllegalArgumentException("Failed to find bundle: $bundleName"))
        }

        val file = baseDir.resolve(remainingPath)
        if (!file.exists() || !file.isFile) {
            return lua.error(IllegalArgumentException("File not found: $path"))
        }

        // Restrict file to files inside baseDir
        if (!file.path.startsWith(baseDir.path)) {
            return lua.error(IllegalArgumentException("Invalid file path: $path"))
        }

        val content = file.readText()
        lua.push(content)
        return 1
    }
}
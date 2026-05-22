package com.seleneworlds.common.bundles

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.system.exitProcess

object LuaTestRunner {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            System.err.println("No Lua test files provided.")
            System.err.println("Usage: runLuaTest --args=\"path/to/test_file.lua [path/to/other_test.lua]\"")
            exitProcess(1)
        }

        val failures = mutableListOf<Pair<String, String>>()
        for (arg in args) {
            try {
                runTestFile(arg)
                println("PASS $arg")
            } catch (t: Throwable) {
                failures += arg to formatFailureMessage(t)
                System.err.println("FAIL $arg")
                System.err.println(formatFailureMessage(t))
            }
        }

        if (failures.isNotEmpty()) {
            System.err.println("${failures.size} Lua test file(s) failed.")
            exitProcess(1)
        }
    }

    private fun runTestFile(testFile: String) {
        val testPath = resolveTestPath(testFile)
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        try {
            luaPackage.initialize(luaManager)
            luaPackage.addPackageResolver { moduleName ->
                val modulePath = resolveModulePath(testPath.parent, moduleName) ?: return@addPackageResolver null
                if (!Files.exists(modulePath)) {
                    return@addPackageResolver null
                }
                modulePath.toString() to modulePath.readText()
            }

            luaManager.lua.load(LuaManager.loadBuffer(testPath.readText()), testFile)
            luaManager.lua.pCall(0, 1)
            luaManager.lua.getField(-1, "run")
            luaManager.lua.pCall(0, 0)
            luaManager.lua.pop(1)
        } finally {
            luaManager.lua.close()
        }
    }

    private fun resolveTestPath(testFile: String): Path {
        val rawPath = Path.of(testFile)
        val resolvedPath = if (rawPath.isAbsolute) rawPath else rawPath.toAbsolutePath()
        require(Files.isRegularFile(resolvedPath)) {
            "Lua test file does not exist: $testFile"
        }
        return resolvedPath.normalize()
    }

    private fun resolveModulePath(startDir: Path, moduleName: String): Path? {
        val relativeModulePath = Path.of(moduleName.replace('.', '/') + ".lua")
        var current: Path? = startDir
        while (current != null) {
            val candidate = current.resolve(relativeModulePath)
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize()
            }
            current = current.parent
        }
        return null
    }

    private fun formatFailureMessage(error: Throwable): String {
        val message = error.message?.trim().orEmpty()
        if (message.isNotEmpty()) {
            return message
        }
        return error::class.qualifiedName ?: error::class.java.name
    }
}

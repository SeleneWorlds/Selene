package com.seleneworlds.server.profiling

import com.seleneworlds.common.bundles.*
import com.seleneworlds.common.jobs.ScheduleEvents
import com.seleneworlds.common.jobs.SchedulesApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.libraries.LuaDebugModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.server.config.ServerConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class ServerSchedulesLuaApiTest {
    @Test
    fun `ordinary schedule callbacks are attributed automatically and retain cancellation semantics`() {
        val root = Files.createTempDirectory("selene-schedule-profile").toFile()
        val profiler = ServerProfiler(ServerConfig(savePath = root.path), LoggerFactory.getLogger(javaClass))
        val dispatcher = MainThreadDispatcher().also { it.bindToCurrentThread() }
        val scheduler = SchedulesApi(dispatcher)
        val manager = LuaManager(LuaPackageModule())
        val owner = Bundle(BundleManifest(name = "test-bundle"), File("."))
        val lua = manager.lua
        val debug = LuaDebugModule().also { it.initialize(manager) }
        lua.push(lua.newTable { debug.register(this) })
        lua.setGlobal("debug")
        fun run(script: String) {
            lua.load(LuaManager.loadBuffer(script), "schedule-profile-test")
            lua.pCall(0, 0)
        }
        try {
            val api = ServerSchedulesLuaApi(scheduler, profiler)
            lua.push(lua.newTable { api.register(this) })
            lua.setGlobal("Schedules")
            profiler.startCapture(60)
            BundleExecutionContext.withBundle(owner) {
                run("""
                    calls = 0
                    local id = Schedules.setInterval(60000, function() calls = calls + 1 end,
                        {immediate = true, name = "normal interval"})
                    Schedules.clearInterval(id)
                    local failed = Schedules.setInterval(60000, function() error("expected failure") end,
                        {immediate = true, name = "failing interval"})
                    Schedules.clearInterval(failed)
                    Schedules.everySecond:connect(function() calls = calls + 1 end)
                    Schedules.setTimeout(5, function() timeoutRan = true end, {name = "normal timeout"})
                    local cancelled = Schedules.setTimeout(60000, function() error("cancelled timeout ran") end)
                    Schedules.clearTimeout(cancelled)
                """.trimIndent())
            }
            // Drop startup periodic signals before verifying a deliberately fired signal.
            dispatcher.process()
            ScheduleEvents.Second.EVENT.invoker().second()
            val deadline = System.nanoTime() + 2_000_000_000L
            var ran = false
            while (!ran && System.nanoTime() < deadline) {
                dispatcher.process()
                lua.getGlobal("timeoutRan")
                ran = lua.toBoolean(-1)
                lua.pop(1)
                if (!ran) Thread.sleep(5)
            }
            assertTrue(ran)
            profiler.stopCapture()
            val rows = profiler.getSummary()["scopes"] as List<*>
            fun row(name: String) = rows.map { it as Map<*, *> }.single { it["name"] == name }
            assertEquals(1L, row("normal interval")["count"])
            assertEquals(1L, row("failing interval")["failures"])
            assertEquals(1L, row("normal timeout")["count"])
            assertTrue((row("everySecond")["count"] as Long) >= 1)
            assertEquals("test-bundle", row("normal interval")["bundle"])
            assertTrue((row("normal interval")["source"] as String).contains("schedule-profile-test"))
            assertFalse(rows.any { (it as Map<*, *>)["name"] == "" })
        } finally {
            scheduler.dispose()
            BundleEventSubscriptions.clearBundleState(owner)
            manager.lua.close()
            profiler.dispose()
            root.deleteRecursively()
        }
    }

    @Test
    fun `optional profiling scopes preserve Lua arguments nil results and exceptions`() {
        val root = Files.createTempDirectory("selene-scope-profile").toFile()
        val profiler = ServerProfiler(ServerConfig(savePath = root.path), LoggerFactory.getLogger(javaClass))
        val manager = LuaManager(LuaPackageModule())
        try {
            val api = ProfilerLuaApi(profiler)
            val lua = manager.lua
            lua.push(lua.newTable { api.register(this) })
            lua.setGlobal("Profiling")
            lua.load(LuaManager.loadBuffer("""
                assert(Profiling.isCapturing() == false)
                local function callback(a,b) return a,nil,b end
                local before = table.pack(Profiling.measure("disabled",callback,17,42))
                assert(before.n == 3 and before[1] == 17 and before[3] == 42)
                Profiling.startCapture(60)
                assert(Profiling.isCapturing() == true)
                local after = table.pack(Profiling.measure("enabled",callback,17,42))
                assert(after.n == 3 and after[1] == 17 and after[2] == nil and after[3] == 42)
                local ok = pcall(Profiling.measure,"failing",function() error("expected") end)
                assert(not ok)
                local summary = Profiling.getSummary()
                assert(summary.active == true and #summary.scopes == 2)
                assert(type(Profiling.stopCapture()) == "string")
                assert(Profiling.isCapturing() == false)
            """.trimIndent()), "optional_profile_scope")
            lua.pCall(0, 0)
        } finally {
            manager.lua.close()
            profiler.dispose()
            root.deleteRecursively()
        }
    }
}

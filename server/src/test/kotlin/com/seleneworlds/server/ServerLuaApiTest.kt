package com.seleneworlds.server

import java.io.File
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.BundleEventSubscriptions
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.data.ServerCustomData
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerLuaApiTest {
    @Test
    fun `bundle file notifications expose sorted Lua arrays`() {
        val luaPackage = LuaPackageModule()
        val manager = LuaManager(luaPackage)
        val module = ServerLuaApi(ServerApi(ServerConfig(), ServerCustomData()))
        val owner = Bundle(BundleManifest(name = "npc-compiler-test"), File("."))
        try {
            manager.lua.push(manager.lua.newTable { module.register(this) })
            manager.lua.setGlobal("Server")
            BundleExecutionContext.withBundle(owner) {
                manager.lua.load(LuaManager.loadBuffer("""
                    Server.bundleFilesChanged:connect(function(bundle, updated, deleted)
                        result = bundle .. ':' .. table.concat(updated, ',') .. ':' .. table.concat(deleted, ',')
                    end)
                """.trimIndent()), "bundle_files_test")
                manager.lua.pCall(0, 0)
            }
            ServerEvents.BundleFilesChanged.EVENT.invoker().bundleFilesChanged(
                "illarion-gobaith", setOf("b.npc", "a.npc"), setOf("old.npc")
            )
            manager.lua.getGlobal("result")
            assertEquals("illarion-gobaith:a.npc,b.npc:old.npc", manager.lua.toString(-1))
            manager.lua.pop(1)
        } finally {
            BundleEventSubscriptions.clearBundleState(owner)
            manager.lua.close()
        }
    }
}

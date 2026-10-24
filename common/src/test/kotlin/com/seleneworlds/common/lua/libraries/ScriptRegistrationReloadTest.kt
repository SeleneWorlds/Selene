package com.seleneworlds.common.lua.libraries

import com.seleneworlds.common.lua.ScriptLifecycleLuaApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.util.newTable
import java.io.File
import kotlin.test.Test

class ScriptRegistrationReloadTest {
    private fun fixture(test: (LuaPackageModule, LuaManager, (String) -> Unit) -> Unit) {
        val packages = LuaPackageModule()
        val manager = LuaManager(packages)
        val lua = manager.lua
        fun run(script: String) {
            lua.load(LuaManager.loadBuffer(script), "test")
            lua.pCall(0, 0)
        }
        try {
            packages.initialize(manager)
            lua.push(packages.packageLoaded)
            lua.push(lua.newTable { ScriptLifecycleLuaApi(packages).register(this) })
            lua.setField(-2, "selene.lifecycle")
            lua.pop(1)
            run("""
                handlers, sent = {}, {}
                package.loaded["selene.network"] = {
                    handlePayload = function(id, handler) handlers[id] = handler end,
                    sendToPlayer = function(player, id, payload) sent[id] = payload end
                }
                package.loaded["selene.event"] = { of = function() return {} end }
                for _, name in ipairs({ "json", "registries", "grid", "permissions" }) do
                    package.loaded["selene." .. name] = {}
                end
                position = function(x, y, z) return { x = x, y = y, z = z } end
                player = {
                    hasRuntimeData = function() return true end,
                    getRuntimeData = function() return { enabled = true } end
                }
                function query()
                    handlers["moonlight-editor:query-context-menu"](player, { x = 0, y = 0, z = 0 })
                    handlers["moonlight-admin:request-actions"](player)
                    return sent["moonlight-editor:context-menu"].actions, sent["moonlight-admin:actions"].actions
                end
            """.trimIndent())
            for ((name, path) in listOf(
                "editor" to "moonlight-editor/server/lua/editor.lua",
                "admin" to "moonlight-admin/server/lua/admin_menu.lua"
            )) {
                val root = listOf(File("bundles/moonlight"), File("../bundles/moonlight"))
                    .first { File(it, path).isFile }
                packages.preloadModule(lua, name, File(root, path).readText())
            }
            run("editor, admin = require('editor'), require('admin')")
            test(packages, manager, ::run)
        } finally {
            lua.close()
        }
    }

    @Test
    fun `reload removes deleted actions and does not duplicate context order`() = fixture { packages, manager, run ->
        fun load(script: String) {
            packages.preloadModule(manager.lua, "contributor", script)
            run("require('contributor')")
        }
        load("""
            local action = { id = 'old', label = 'Old', execute = function() end }
            editor.registerContextMenuAction(action)
            editor.registerContextMenuAction(action)
            admin.registerAction(action)
        """.trimIndent())
        run("local context, admin = query(); assert(#context == 1); assert(#admin == 1)")
        repeat(3) {
            packages.clearLoadedModule(manager.lua, "contributor")
            load("""
                local action = { id = 'new', label = 'New', execute = function() end }
                editor.registerContextMenuAction(action)
                admin.registerAction(action)
            """.trimIndent())
            run("local context, admin = query(); assert(#context == 1 and context[1].id == 'new'); assert(#admin == 1 and admin[1].id == 'new')")
        }
        packages.clearLoadedModule(manager.lua, "contributor")
        run("local context, admin = query(); assert(#context == 0); assert(#admin == 0)")
    }

    @Test
    fun `nested requires keep independent ownership and cleanup preserves replacements`() = fixture { packages, manager, run ->
        packages.preloadModule(manager.lua, "child", """
            editor.registerContextMenuAction({ id = 'shared', label = 'Child', execute = function() end })
            admin.registerAction({ id = 'shared', label = 'Child', execute = function() end })
        """.trimIndent())
        packages.withScript("parent") {
            run("""
                require('child')
                editor.registerContextMenuAction({ id = 'shared', label = 'Parent', execute = function() end })
                admin.registerAction({ id = 'shared', label = 'Parent', execute = function() end })
            """.trimIndent())
        }
        packages.clearLoadedModule(manager.lua, "child")
        run("local context, admin = query(); assert(#context == 1 and context[1].label == 'Parent'); assert(#admin == 1 and admin[1].label == 'Parent')")
        packages.clearLoadedModule(manager.lua, "parent")
        run("local context, admin = query(); assert(#context == 0); assert(#admin == 0)")
    }
    @Test
    fun `providers are removed on unload and a failed load leaves no partial actions`() = fixture { packages, manager, run ->
        packages.preloadModule(manager.lua, "providers", """
            editor.registerGoToProvider(function()
                return { { name = 'Place', type = 'test', coordinate = { x = 1, y = 2, z = 3 } } }
            end)
            editor.registerGizmoProvider(function()
                return { { id = 'gizmo', coordinate = { x = 1, y = 2, z = 3 } } }
            end)
        """.trimIndent())
        run("""
            require('providers')
            handlers['moonlight-editor:request-go-to-targets'](player)
            assert(#sent['moonlight-editor:go-to-targets'].targets == 1)
            handlers['moonlight-editor:request-gizmos'](player, { x = 0, y = 0, z = 0 })
            assert(sent['moonlight-editor:gizmos-start'].count == 1)
        """.trimIndent())
        packages.clearLoadedModule(manager.lua, "providers")
        run("""
            sent['moonlight-editor:go-to-targets'] = nil
            handlers['moonlight-editor:request-go-to-targets'](player)
            assert(sent['moonlight-editor:go-to-targets'] == nil)
            handlers['moonlight-editor:request-gizmos'](player, { x = 0, y = 0, z = 0 })
            assert(sent['moonlight-editor:gizmos-start'].count == 0)
        """.trimIndent())
        packages.preloadModule(manager.lua, "broken", """
            editor.registerContextMenuAction({ id = 'partial', label = 'Partial', execute = function() end })
            admin.registerAction({ id = 'partial', label = 'Partial', execute = function() end })
            error('load failed')
        """.trimIndent())
        run("""
            assert(not pcall(require, 'broken'))
            local context, admin = query()
            assert(#context == 0 and #admin == 0)
        """.trimIndent())
    }

}

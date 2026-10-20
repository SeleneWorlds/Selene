package com.seleneworlds.server.http

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.util.xpCall

/** Invoked on the main thread by the authenticated resource routes. */
class ScriptSchemas(private val luaManager: LuaManager) {
    fun fetch(module: String, functionName: String): String {
        val lua = luaManager.lua
        val top = lua.top
        try {
            lua.load(LuaManager.loadBuffer("""
                local module, functionName = ...
                local editor = require("moonlight-editor.server.lua.editor")
                return require("selene.json").encode(editor.scriptedSchema(module, functionName))
            """.trimIndent()), "http-script-schema")
            lua.push(module)
            lua.push(functionName)
            lua.xpCall(2, 1)
            return requireNotNull(lua.toString(-1)) { "Schema provider did not return JSON" }
        } finally {
            lua.top = top
        }
    }
}

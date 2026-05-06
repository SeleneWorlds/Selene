package com.seleneworlds.server.script

interface ServerScriptProvider {
    fun loadEntityScript(module: String): ServerEntityScript
}

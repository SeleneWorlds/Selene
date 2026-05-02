package com.seleneworlds.client.script

interface ClientScriptProvider {
    fun loadEntityScript(module: String): ClientEntityScript
}
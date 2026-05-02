package com.seleneworlds.server.config

class ConfigApi(private val scriptProperties: ScriptProperties) {
    fun getProperty(key: String): String? = scriptProperties.getProperty(key)
}

package com.seleneworlds.server

import com.seleneworlds.server.config.ServerConfig

class ServerApi(
    private val serverConfig: ServerConfig
) {
    fun getName(): String {
        return serverConfig.name
    }
}

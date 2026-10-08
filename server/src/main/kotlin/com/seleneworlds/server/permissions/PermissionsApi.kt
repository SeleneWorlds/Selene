package com.seleneworlds.server.permissions

import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.server.players.PlayerApi
import org.slf4j.LoggerFactory

/** General player permission policies. Register and check on the server main thread. */
class PermissionsApi {
    private var handler: ((PlayerApi, String, SerializedMap) -> Boolean)? = null
    private val logger = LoggerFactory.getLogger(PermissionsApi::class.java)

    fun setHandler(handler: (PlayerApi, String, SerializedMap) -> Boolean) {
        this.handler = handler
    }

    /** Missing handlers and failed policies deny access. */
    fun hasPermission(player: PlayerApi, permission: String, context: SerializedMap = emptyMap()): Boolean {
        val handler = handler ?: return false
        return try {
            handler(player, permission, context)
        } catch (error: Exception) {
            logger.error("Permission check failed: $permission", error)
            false
        }
    }

}
